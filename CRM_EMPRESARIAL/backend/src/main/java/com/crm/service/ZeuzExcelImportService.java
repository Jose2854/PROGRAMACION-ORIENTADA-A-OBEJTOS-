package com.crm.service;

import com.crm.dto.ZeuzImportResult;
import com.crm.entity.Cliente;
import com.crm.entity.Factura;
import com.crm.entity.FacturaDetalle;
import com.crm.entity.Venta;
import com.crm.repository.ClienteRepository;
import com.crm.repository.FacturaDetalleRepository;
import com.crm.repository.FacturaRepository;
import com.crm.repository.VentaRepository;
import com.crm.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ZeuzExcelImportService {

    private static final String SOURCE = "ZEUZ_EXCEL";
    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "factura", "Numero", "Fecha", "Nombre Vendedor", "Records", "Item", "TipoServicio", "Documento", "Ruta", "Revisado",
            "Pasajero", "Tarifa", "Iva", "Otros", "Descuentos",
            "FechaSalida_CheckIN", "FechaLlegada_CheckOut", "FechaExpedicion", "EntVendedora");

    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final FacturaRepository facturaRepository;
    private final FacturaDetalleRepository facturaDetalleRepository;

    @Transactional
    public ZeuzImportResult importFile(MultipartFile file) {
        validateFile(file);
        // Los reportes Fare Basis de varios meses incluyen una tabla de textos
        // comprimida que supera el límite predeterminado de POI (100 MB).
        IOUtils.setByteArrayMaxOverride(250_000_000);
        Long tenantId = TenantContext.requireCurrentTenant();
        Map<String, Cliente> clientsByZeuzCode = new HashMap<>();
        clienteRepository.findByTenantId(tenantId).stream()
                .filter(client -> client.getIdentificacion() != null && !client.getIdentificacion().isBlank())
                .forEach(client -> clientsByZeuzCode.put(client.getIdentificacion(), client));
        Set<String> existingIds = ventaRepository.findExternalIdsByTenantIdAndBackofficeOrigen(tenantId, SOURCE);
        List<Venta> pending = new ArrayList<>();
        Map<String, Venta> salesByDocument = new HashMap<>();
        Map<String, InvoiceAccumulator> invoices = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        int read = 0;
        int skipped = 0;
        int errors = 0;
        BigDecimal importedTotal = BigDecimal.ZERO;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            SheetLayout layout = findSalesSheet(workbook);
            Sheet sheet = layout.sheet();
            Map<String, Integer> columns = layout.columns();
            List<String> missing = REQUIRED_HEADERS.stream().filter(header -> !columns.containsKey(header)).sorted().toList();
            if (!missing.isEmpty()) throw new IllegalArgumentException("El archivo Zeuz no contiene las columnas: " + String.join(", ", missing));

            DataFormatter formatter = new DataFormatter();
            for (int index = layout.headerRow() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || isBlank(row)) continue;
                read++;
                try {
                    if (!isEligibleTicket(row, columns, formatter)) {
                        skipped++;
                        continue;
                    }
                    String document = text(row, columns, "Documento", formatter);
                    if (document.isBlank()) throw new IllegalArgumentException("Documento vacío");
                    Cliente client = findOrCreateZeuzClient(row, columns, formatter, tenantId, clientsByZeuzCode);
                    accumulateInvoice(row, columns, formatter, client, invoices, warnings, index + 1);
                    if (existingIds.contains(document)) {
                        skipped++;
                        continue;
                    }
                    Venta sale = map(row, columns, formatter, tenantId, client, document, warnings, index + 1);
                    pending.add(sale);
                    salesByDocument.put(document, sale);
                    existingIds.add(document);
                    importedTotal = importedTotal.add(sale.getTotal() == null ? BigDecimal.ZERO : sale.getTotal());
                } catch (RuntimeException error) {
                    errors++;
                    if (warnings.size() < 20) warnings.add("Fila " + (index + 1) + ": " + error.getMessage());
                }
            }
            ventaRepository.saveAll(pending);
            synchronizeInvoices(tenantId, invoices, salesByDocument);
        } catch (IOException error) {
            throw new IllegalArgumentException("No fue posible leer el archivo Excel", error);
        }

        return new ZeuzImportResult(read, pending.size(), skipped, errors, importedTotal, warnings);
    }

    private boolean isEligibleTicket(Row row, Map<String, Integer> columns, DataFormatter formatter) {
        String reviewed = text(row, columns, "Revisado", formatter);
        if (!reviewed.isBlank()) return false;

        String item = normalizeCategory(text(row, columns, "Item", formatter));
        String serviceType = normalizeCategory(text(row, columns, "TipoServicio", formatter));
        return "TIQUETE".equals(item) && !serviceType.contains("AEREO NACIONAL");
    }

    private String normalizeCategory(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return normalized.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private Venta map(Row row, Map<String, Integer> columns, DataFormatter formatter, Long tenantId,
                      Cliente client, String document, List<String> warnings, int rowNumber) {
        Venta sale = new Venta();
        sale.setTenantId(tenantId);
        sale.setCliente(client);
        sale.setCodigo("ZEUZ-" + document);
        sale.setExternalId(document);
        sale.setBackofficeOrigen(SOURCE);
        sale.setDescripcion(text(row, columns, "DescripcionCompleta", formatter));
        sale.setTipoProductoTuristico(Venta.TipoProductoTuristico.VUELO);
        sale.setEstado(Venta.EstadoVenta.CERRADA);
        sale.setMetodoPago(Venta.MetodoPago.TRANSFERENCIA);
        sale.setMoneda("COP");
        sale.setNumeroPasajeros(1);
        sale.setVendedor(text(row, columns, "Nombre Vendedor", formatter));
        sale.setLocalizador(text(row, columns, "Records", formatter));
        sale.setPasajero(text(row, columns, "Pasajero", formatter));
        sale.setClaseTarifaria(text(row, columns, "Clase", formatter));
        sale.setTarifa(decimal(row, columns, "Tarifa"));
        sale.setCombustible(decimal(row, columns, "Combustible"));
        sale.setImpuesto(decimal(row, columns, "Iva"));
        sale.setOtros(decimal(row, columns, "Otros"));
        sale.setDescuento(decimal(row, columns, "Descuentos"));
        sale.setMonto(sale.getTarifa());
        // COLAEREO mide sus ventas por la tarifa base. Los impuestos y otros cargos
        // se conservan para consulta, pero no se suman al ingreso analítico.
        sale.setTotal(sale.getTarifa());

        String route = text(row, columns, "Ruta", formatter);
        String[] stops = route.split("/");
        sale.setOrigen(stops.length == 0 ? route : stops[0].trim());
        sale.setDestino(stops.length == 0 ? route : stops[stops.length - 1].trim());
        sale.setNotas("Ruta: " + route + " | " + text(row, columns, "TipoServicio", formatter));

        String seller = text(row, columns, "EntVendedora", formatter);
        sale.setEntidadVendedora(seller);
        String airline = stripEntityCode(seller);
        sale.setProveedor(airline);
        sale.setAerolinea(airline);
        sale.setEntidadDistribuidora(text(row, columns, "EntDistribuidora", formatter));
        sale.setNacionalidad(text(row, columns, "Nacionalidad", formatter));
        sale.setTiqueteador(text(row, columns, "Tiqueteador", formatter));
        sale.setVuelo(text(row, columns, "Vuelo", formatter));
        sale.setFareBasis(text(row, columns, "FAREBASIS", formatter));

        LocalDate saleDate = date(row, columns, "Fecha");
        sale.setFechaVentaBackoffice(saleDate);
        sale.setFechaCierre(saleDate == null ? LocalDateTime.now() : saleDate.atTime(LocalTime.NOON));
        sale.setFechaViajeInicio(date(row, columns, "FechaSalida_CheckIN"));
        sale.setFechaViajeFin(date(row, columns, "FechaLlegada_CheckOut"));
        LocalDate issueDate = date(row, columns, "FechaExpedicion");
        if (issueDate != null && issueDate.isAfter(LocalDate.now().plusYears(5))) {
            if (warnings.size() < 20) warnings.add("Fila " + rowNumber + ": fecha de expedición atípica " + issueDate);
        }
        sale.setFechaExpedicion(issueDate);
        return sale;
    }

    private void accumulateInvoice(Row row, Map<String, Integer> columns, DataFormatter formatter,
                                   Cliente client, Map<String, InvoiceAccumulator> invoices,
                                   List<String> warnings, int rowNumber) {
        String invoiceNumber = text(row, columns, "factura", formatter);
        if (invoiceNumber.isBlank()) {
            if (warnings.size() < 20) warnings.add("Fila " + rowNumber + ": número de factura vacío");
            return;
        }

        InvoiceAccumulator invoice = invoices.computeIfAbsent(invoiceNumber,
                ignored -> new InvoiceAccumulator(invoiceNumber,
                        text(row, columns, "Numero", formatter), client,
                        text(row, columns, "Nombre Vendedor", formatter),
                        date(row, columns, "Fecha")));
        String document = text(row, columns, "Documento", formatter);
        if (document.isBlank()) return;
        String seller = text(row, columns, "EntVendedora", formatter);
        invoice.details.put(document, new InvoiceDetailDraft(
                document,
                text(row, columns, "Pasajero", formatter),
                text(row, columns, "Ruta", formatter),
                stripEntityCode(seller),
                text(row, columns, "Records", formatter),
                date(row, columns, "FechaSalida_CheckIN"),
                decimal(row, columns, "Tarifa")));
    }

    private void synchronizeInvoices(Long tenantId, Map<String, InvoiceAccumulator> invoices,
                                     Map<String, Venta> salesByDocument) {
        Map<String, Factura> existingInvoices = new HashMap<>();
        facturaRepository.findByTenantIdAndFuente(tenantId, SOURCE)
                .forEach(invoice -> existingInvoices.put(invoice.getNumero(), invoice));

        Map<String, Factura> invoicesByNumber = new LinkedHashMap<>();
        List<Factura> invoiceBatch = new ArrayList<>();
        for (InvoiceAccumulator source : invoices.values()) {
            Factura invoice = existingInvoices.getOrDefault(source.number, new Factura());
            boolean isNew = invoice.getId() == null;
            invoice.setTenantId(tenantId);
            invoice.setNumero(source.number);
            invoice.setNumeroZeuz(source.zeuzNumber);
            invoice.setFuente(SOURCE);
            invoice.setTipo("VENTA_ZEUZ");
            if (isNew) invoice.setEstado("PENDIENTE");
            invoice.setClienteId(source.client.getId());
            invoice.setVendedor(source.seller);
            invoice.setFechaEmision(source.issueDate);
            invoice.setMoneda("COP");
            invoice.setImpuesto(BigDecimal.ZERO);
            invoice.setDescripcion("Factura importada desde reporte Fare Basis de Zeuz");
            invoice.setNotas("El valor corresponde a la suma de la tarifa base y no incluye impuestos.");
            invoiceBatch.add(invoice);
            invoicesByNumber.put(source.number, invoice);
        }
        facturaRepository.saveAll(invoiceBatch);

        List<Long> invoiceIds = invoiceBatch.stream().map(Factura::getId).filter(Objects::nonNull).toList();
        Map<Long, Map<String, FacturaDetalle>> detailsByInvoice = new HashMap<>();
        if (!invoiceIds.isEmpty()) {
            facturaDetalleRepository.findByTenantIdAndFacturaIdIn(tenantId, invoiceIds)
                    .forEach(detail -> detailsByInvoice
                            .computeIfAbsent(detail.getFacturaId(), ignored -> new LinkedHashMap<>())
                            .put(detail.getDocumento(), detail));
        }

        List<FacturaDetalle> changedDetails = new ArrayList<>();
        for (InvoiceAccumulator source : invoices.values()) {
            Factura invoice = invoicesByNumber.get(source.number);
            Map<String, FacturaDetalle> detailsByDocument = detailsByInvoice
                    .computeIfAbsent(invoice.getId(), ignored -> new LinkedHashMap<>());
            for (InvoiceDetailDraft draft : source.details.values()) {
                FacturaDetalle detail = detailsByDocument.getOrDefault(draft.document, new FacturaDetalle());
                detail.setTenantId(tenantId);
                detail.setFacturaId(invoice.getId());
                Venta sale = salesByDocument.get(draft.document);
                if (sale != null && sale.getId() != null) detail.setVentaId(sale.getId());
                detail.setDocumento(draft.document);
                detail.setPasajero(draft.passenger);
                detail.setRuta(draft.route);
                detail.setAerolinea(draft.airline);
                detail.setLocalizador(draft.locator);
                detail.setFechaViaje(draft.travelDate);
                detail.setTarifa(draft.fare);
                detail.setMoneda("COP");
                changedDetails.add(detail);
                detailsByDocument.put(draft.document, detail);
            }

            BigDecimal fareTotal = detailsByDocument.values().stream()
                    .map(FacturaDetalle::getTarifa)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            invoice.setCantidadItems(detailsByDocument.size());
            invoice.setSubtotal(fareTotal);
            invoice.setTotal(fareTotal);
        }
        facturaDetalleRepository.saveAll(changedDetails);
        facturaRepository.saveAll(invoiceBatch);
    }

    private Cliente findOrCreateZeuzClient(Row row, Map<String, Integer> columns, DataFormatter formatter,
                                            Long tenantId, Map<String, Cliente> clientsByZeuzCode) {
        String code = text(row, columns, "Codigo del Cliente", formatter);
        String fullName = text(row, columns, "Nombre del Cliente", formatter);
        if (code.isBlank() && fullName.isBlank()) {
            code = "ZEUZ-SIN-CLIENTE";
            fullName = "Cliente no informado";
        } else if (code.isBlank()) {
            code = "Z-" + Integer.toUnsignedString(fullName.toUpperCase().hashCode());
        } else if (fullName.isBlank()) {
            fullName = "Cliente Zeuz " + code;
        }

        final String clientCode = code;
        final String clientName = fullName;
        Cliente client = clientsByZeuzCode.get(clientCode);
        if (client == null) {
            String[] nameParts = clientName.trim().split("\\s+", 2);
            client = new Cliente();
            client.setTenantId(tenantId);
            client.setNombre(nameParts[0]);
            client.setApellido(nameParts.length > 1 ? nameParts[1] : "-");
            client.setIdentificacion(clientCode);
            client.setTipoIdentificacion("ZEUZ");
            client.setEmpresa(clientName.trim());
            client.setEstado(Cliente.EstadoCliente.ACTIVO);
            client.setEstadoNegociacion(Cliente.EstadoNegociacion.SIN_NEGOCIACION);
            client.setTieneCartera(false);
            client.setNotas("Cliente creado automáticamente desde las columnas H e I de Zeuz.");
        }

        String sellerName = text(row, columns, "Nombre Vendedor", formatter);
        Cliente.TipoGestionComercial commercialType = sameCommercialName(clientName, sellerName)
                ? Cliente.TipoGestionComercial.AGENCIA
                : Cliente.TipoGestionComercial.FREELANCE;
        String commercialOwner = commercialType == Cliente.TipoGestionComercial.AGENCIA
                ? clientName.trim() : sellerName.trim();
        if (commercialOwner.isBlank()) {
            commercialType = Cliente.TipoGestionComercial.SIN_CLASIFICAR;
            commercialOwner = null;
        }

        boolean changed = client.getId() == null
                || client.getTipoGestionComercial() != commercialType
                || !Objects.equals(client.getResponsableComercial(), commercialOwner);
        client.setTipoGestionComercial(commercialType);
        client.setResponsableComercial(commercialOwner);
        if (changed) client = clienteRepository.save(client);
        clientsByZeuzCode.put(clientCode, client);
        return client;
    }

    private boolean sameCommercialName(String clientName, String sellerName) {
        return normalizeCommercialName(clientName).equals(normalizeCommercialName(sellerName));
    }

    private String normalizeCommercialName(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toUpperCase();
    }

    private Map<String, Integer> headers(Row headerRow) {
        if (headerRow == null) throw new IllegalArgumentException("El archivo no tiene encabezados");
        Map<String, Integer> result = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : headerRow) result.put(formatter.formatCellValue(cell).trim(), cell.getColumnIndex());
        return result;
    }

    private SheetLayout findSalesSheet(Workbook workbook) {
        for (Sheet sheet : workbook) {
            int lastCandidate = Math.min(sheet.getLastRowNum(), 25);
            for (int rowIndex = 0; rowIndex <= lastCandidate; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                Map<String, Integer> columns = headers(row);
                if (columns.keySet().containsAll(REQUIRED_HEADERS)) {
                    return new SheetLayout(sheet, rowIndex, columns);
                }
            }
        }
        throw new IllegalArgumentException("No se encontró una hoja de ventas de Zeuz con los encabezados requeridos");
    }

    private String text(Row row, Map<String, Integer> columns, String name, DataFormatter formatter) {
        Integer index = columns.get(name);
        if (index == null) return "";
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private BigDecimal decimal(Row row, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        if (index == null) return BigDecimal.ZERO;
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return BigDecimal.ZERO;
        if (cell.getCellType() == CellType.NUMERIC) return BigDecimal.valueOf(cell.getNumericCellValue());
        String value = cell.toString().trim().replace(",", "");
        return value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value);
    }

    private LocalDate date(Row row, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        if (index == null) return null;
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isValidExcelDate(cell.getNumericCellValue())) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        String value = cell.toString().trim();
        if (value.isBlank()) return null;
        for (DateTimeFormatter pattern : List.of(DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))) {
            try { return LocalDateTime.parse(value, pattern).toLocalDate(); }
            catch (DateTimeParseException ignored) { }
        }
        for (DateTimeFormatter pattern : List.of(DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("d/M/yyyy"))) {
            try { return LocalDate.parse(value, pattern); }
            catch (DateTimeParseException ignored) { }
        }
        throw new IllegalArgumentException("Fecha inválida en " + name + ": " + value);
    }

    private String stripEntityCode(String value) {
        return value.replaceFirst("^\\s*\\d+\\s*-\\s*", "").trim();
    }

    private boolean isBlank(Row row) {
        for (Cell cell : row) if (cell.getCellType() != CellType.BLANK && !cell.toString().isBlank()) return false;
        return true;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Selecciona un archivo Excel");
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx") && !name.endsWith(".xlsm")) {
            throw new IllegalArgumentException("El reporte debe estar en formato .xlsx o .xlsm");
        }
    }

    private static final class InvoiceAccumulator {
        private final String number;
        private final String zeuzNumber;
        private final Cliente client;
        private final String seller;
        private final LocalDate issueDate;
        private final Map<String, InvoiceDetailDraft> details = new LinkedHashMap<>();

        private InvoiceAccumulator(String number, String zeuzNumber, Cliente client, String seller, LocalDate issueDate) {
            this.number = number;
            this.zeuzNumber = zeuzNumber;
            this.client = client;
            this.seller = seller;
            this.issueDate = issueDate;
        }
    }

    private record InvoiceDetailDraft(String document, String passenger, String route, String airline,
                                      String locator, LocalDate travelDate, BigDecimal fare) { }

    private record SheetLayout(Sheet sheet, int headerRow, Map<String, Integer> columns) { }
}
