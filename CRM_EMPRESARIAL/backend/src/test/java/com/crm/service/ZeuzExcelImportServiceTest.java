package com.crm.service;

import com.crm.dto.ZeuzImportResult;
import com.crm.entity.Cliente;
import com.crm.entity.Venta;
import com.crm.repository.ClienteRepository;
import com.crm.repository.FacturaDetalleRepository;
import com.crm.repository.FacturaRepository;
import com.crm.repository.VentaRepository;
import com.crm.security.TenantContext;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ZeuzExcelImportServiceTest {

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void mapsRealZeuzColumnsAndAvoidsDiscardedColumns() throws Exception {
        VentaRepository sales = mock(VentaRepository.class);
        ClienteRepository clients = mock(ClienteRepository.class);
        FacturaRepository invoices = mock(FacturaRepository.class);
        FacturaDetalleRepository invoiceDetails = mock(FacturaDetalleRepository.class);
        when(clients.findByTenantId(7L)).thenReturn(List.of());
        when(clients.save(org.mockito.ArgumentMatchers.any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sales.findExternalIdsByTenantIdAndBackofficeOrigen(7L, "ZEUZ_EXCEL")).thenReturn(new HashSet<>());
        when(sales.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoices.findByTenantIdAndFuenteAndNumero(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq("ZEUZ_EXCEL"), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        when(invoices.findByTenantIdAndFuente(7L, "ZEUZ_EXCEL")).thenReturn(List.of());
        when(invoices.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            com.crm.entity.Factura invoice = invocation.getArgument(0);
            if (invoice.getId() == null) invoice.setId(1L);
            return invoice;
        });
        when(invoices.saveAll(anyList())).thenAnswer(invocation -> {
            List<com.crm.entity.Factura> batch = invocation.getArgument(0);
            for (int i = 0; i < batch.size(); i++) if (batch.get(i).getId() == null) batch.get(i).setId((long) i + 1);
            return batch;
        });
        when(invoiceDetails.findByTenantIdAndFacturaIdOrderByIdAsc(7L, 1L)).thenReturn(List.of());
        when(invoiceDetails.findByTenantIdAndFacturaIdIn(org.mockito.ArgumentMatchers.eq(7L), anyList())).thenReturn(List.of());
        when(invoiceDetails.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        TenantContext.setCurrentTenant(7L);

        String[] headers = {"factura", "Numero", "Fecha", "Codigo del Cliente", "Nombre del Cliente", "Nombre Vendedor", "Records", "Item", "TipoServicio", "Documento", "Ruta", "Revisado",
                "DescripcionCompleta", "Clase", "Pasajero", "Tarifa", "Combustible", "Iva", "Otros",
                "Descuentos", "Total", "FechaSalida_CheckIN", "FechaLlegada_CheckOut", "FechaExpedicion",
                "Nacionalidad", "Tiqueteador", "EntVendedora", "EntDistribuidora", "Vuelo", "FAREBASIS"};
        byte[] bytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.createSheet("Z®Ventas Detalladas").createRow(0).createCell(0).setCellValue("Parámetros");
            Sheet sheet = workbook.createSheet("prueba");
            Row header = sheet.createRow(1);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            Row row = sheet.createRow(2);
            String[] values = {"340100000001", "0000000001", "2026-08-01", "CLI-001", "EMPRESA TURISTICA PRUEBA", "Asesor Uno", "ABC123", "Tiquete", "Tiquete Aire Internacional", "4856414842",
                    "MDE/BOG/MAD", "", "Boleto aéreo", "Y", "PASAJERO PRUEBA", "100000", "0", "19000", "5000",
                    "0", "124000", "2026-09-01", "2026-09-02", "2026-08-01", "Internacional", "Asesor Uno",
                    "134 - AVIANCA", "BSP - BSP", "1234", "YTEST"};
            for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);

            Row reviewedRow = sheet.createRow(3);
            for (int i = 0; i < values.length; i++) reviewedRow.createCell(i).setCellValue(values[i]);
            reviewedRow.getCell(9).setCellValue("DOC-REVISADO");
            reviewedRow.getCell(11).setCellValue("123456");

            Row nonTicketRow = sheet.createRow(4);
            for (int i = 0; i < values.length; i++) nonTicketRow.createCell(i).setCellValue(values[i]);
            nonTicketRow.getCell(7).setCellValue("Servicio");
            nonTicketRow.getCell(8).setCellValue("AEREO NACIONAL");
            nonTicketRow.getCell(9).setCellValue("DOC-SERVICIO");
            workbook.write(output);
            bytes = output.toByteArray();
        }

        ZeuzImportResult result = new ZeuzExcelImportService(sales, clients, invoices, invoiceDetails).importFile(
                new MockMultipartFile("file", "ventas.xlsm", "application/vnd.ms-excel.sheet.macroEnabled.12", bytes));

        ArgumentCaptor<List<Venta>> captor = ArgumentCaptor.forClass(List.class);
        verify(sales).saveAll(captor.capture());
        Venta imported = captor.getValue().get(0);
        assertEquals(1, result.ventasImportadas());
        assertEquals(2, result.duplicadosOmitidos());
        assertEquals("4856414842", imported.getExternalId());
        assertEquals("MDE", imported.getOrigen());
        assertEquals("MAD", imported.getDestino());
        assertEquals("AVIANCA", imported.getAerolinea());
        assertEquals("CLI-001", imported.getCliente().getIdentificacion());
        assertEquals("EMPRESA", imported.getCliente().getNombre());
        assertEquals("TURISTICA PRUEBA", imported.getCliente().getApellido());
        assertEquals(Cliente.TipoGestionComercial.FREELANCE, imported.getCliente().getTipoGestionComercial());
        assertEquals("Asesor Uno", imported.getCliente().getResponsableComercial());
        assertEquals(LocalDate.of(2026, 9, 1), imported.getFechaViajeInicio());
        assertEquals(0, imported.getTarifa().compareTo(new java.math.BigDecimal("100000.0")));
        assertEquals(0, imported.getMonto().compareTo(new java.math.BigDecimal("100000.0")));
        assertEquals(0, imported.getTotal().compareTo(new java.math.BigDecimal("100000.0")));
        assertEquals(0, result.totalImportado().compareTo(new java.math.BigDecimal("100000.0")));
    }

    @Test
    void classifiesAgencyWhenClientAndSellerNamesMatch() throws Exception {
        VentaRepository sales = mock(VentaRepository.class);
        ClienteRepository clients = mock(ClienteRepository.class);
        FacturaRepository invoices = mock(FacturaRepository.class);
        FacturaDetalleRepository invoiceDetails = mock(FacturaDetalleRepository.class);
        when(clients.findByTenantId(7L)).thenReturn(List.of());
        when(clients.save(org.mockito.ArgumentMatchers.any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sales.findExternalIdsByTenantIdAndBackofficeOrigen(7L, "ZEUZ_EXCEL")).thenReturn(new HashSet<>());
        when(sales.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoices.findByTenantIdAndFuenteAndNumero(org.mockito.ArgumentMatchers.eq(7L), org.mockito.ArgumentMatchers.eq("ZEUZ_EXCEL"), org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());
        when(invoices.findByTenantIdAndFuente(7L, "ZEUZ_EXCEL")).thenReturn(List.of());
        when(invoices.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            com.crm.entity.Factura invoice = invocation.getArgument(0);
            if (invoice.getId() == null) invoice.setId(1L);
            return invoice;
        });
        when(invoices.saveAll(anyList())).thenAnswer(invocation -> {
            List<com.crm.entity.Factura> batch = invocation.getArgument(0);
            for (int i = 0; i < batch.size(); i++) if (batch.get(i).getId() == null) batch.get(i).setId((long) i + 1);
            return batch;
        });
        when(invoiceDetails.findByTenantIdAndFacturaIdOrderByIdAsc(7L, 1L)).thenReturn(List.of());
        when(invoiceDetails.findByTenantIdAndFacturaIdIn(org.mockito.ArgumentMatchers.eq(7L), anyList())).thenReturn(List.of());
        when(invoiceDetails.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        TenantContext.setCurrentTenant(7L);

        String[] headers = {"factura", "Numero", "Fecha", "Codigo del Cliente", "Nombre del Cliente", "Nombre Vendedor", "Records", "Item", "TipoServicio", "Documento", "Ruta", "Revisado",
                "Pasajero", "Tarifa", "Iva", "Otros", "Descuentos", "FechaSalida_CheckIN", "FechaLlegada_CheckOut", "FechaExpedicion", "EntVendedora"};
        byte[] bytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("ventas");
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            Row row = sheet.createRow(1);
            String[] values = {"340100000002", "0000000002", "2026-08-01", "AGE-001", "AGENCIA EJEMPLO", "  agencia ejemplo  ", "ABC123", "Tiquete", "Tiquete Aire Internacional", "DOC-1", "BOG/MDE", "",
                    "PASAJERO", "100000", "19000", "0", "0", "2026-09-01", "2026-09-02", "2026-08-01", "134 - AVIANCA"};
            for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);
            workbook.write(output);
            bytes = output.toByteArray();
        }

        new ZeuzExcelImportService(sales, clients, invoices, invoiceDetails).importFile(
                new MockMultipartFile("file", "ventas.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes));

        ArgumentCaptor<List<Venta>> captor = ArgumentCaptor.forClass(List.class);
        verify(sales).saveAll(captor.capture());
        Cliente agency = captor.getValue().get(0).getCliente();
        assertEquals(Cliente.TipoGestionComercial.AGENCIA, agency.getTipoGestionComercial());
        assertEquals("AGENCIA EJEMPLO", agency.getResponsableComercial());
    }
}
