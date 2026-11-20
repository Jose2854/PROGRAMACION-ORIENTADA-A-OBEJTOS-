/*
 * CRM SaaS - Copyright (c) 2024-2026 Hector Andres Ladino
 * Licensed under MIT License. See LICENSE file for details.
 */
import { useEffect, useMemo, useRef, useState } from 'react';
import { AlertCircle, CheckCircle2, Edit, Eye, FileSpreadsheet, Plus, Receipt, Search, Trash2, Upload } from 'lucide-react';
import apiClient from '../services/api';
import { Factura } from '../types';
import Button from '../components/Button';
import Input from '../components/Input';
import Modal from '../components/Modal';

const emptyInvoice: Factura = {
  numero: '', descripcion: '', tipo: 'VENTA', estado: 'PENDIENTE', clienteId: 0, ventaId: 0,
  fechaEmision: '', fechaVencimiento: '', fechaPago: '', subtotal: 0, impuesto: 0, total: 0,
  moneda: 'COP', metodoPago: 'TRANSFERENCIA', urlFactura: '', notas: '',
};

const money = (value?: number, currency = 'COP') => new Intl.NumberFormat('es-CO', {
  style: 'currency', currency: currency || 'COP', maximumFractionDigits: 0,
}).format(value || 0);

const date = (value?: string) => value ? new Date(`${value}T12:00:00`).toLocaleDateString('es-CO') : '—';

const statusClass = (status: string) => ({
  PAGADA: 'bg-emerald-100 text-emerald-800', VENCIDA: 'bg-red-100 text-red-800',
  ANULADA: 'bg-slate-100 text-slate-700', PENDIENTE: 'bg-amber-100 text-amber-800',
}[status] || 'bg-blue-100 text-blue-800');

const FacturasPage = () => {
  const [facturas, setFacturas] = useState<Factura[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingFactura, setEditingFactura] = useState<Factura | null>(null);
  const [detailFactura, setDetailFactura] = useState<Factura | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('TODAS');
  const [loading, setLoading] = useState(true);
  const [importing, setImporting] = useState(false);
  const [notice, setNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [summary, setSummary] = useState({ cantidad: 0, total: 0, pendiente: 0, pagada: 0 });
  const [formData, setFormData] = useState<Factura>(emptyInvoice);
  const fileInput = useRef<HTMLInputElement>(null);

  const loadFacturas = async () => {
    setLoading(true);
    try {
      const [response, totals] = await Promise.all([
        apiClient.get('/api/facturas'),
        apiClient.get('/api/facturas/resumen'),
      ]);
      setFacturas(response.data);
      setSummary(totals.data);
    } catch (error) {
      console.error('Error loading facturas:', error);
      setNotice({ type: 'error', text: 'No fue posible cargar las facturas.' });
    } finally { setLoading(false); }
  };

  useEffect(() => { loadFacturas(); }, []);

  const filteredFacturas = useMemo(() => facturas.filter((item) => {
    const term = searchTerm.trim().toLowerCase();
    const matchesSearch = !term || [item.numero, item.numeroZeuz, item.clienteNombre, item.vendedor]
      .some(value => (value || '').toLowerCase().includes(term));
    return matchesSearch && (statusFilter === 'TODAS' || item.estado === statusFilter);
  }), [facturas, searchTerm, statusFilter]);
  const visibleFacturas = filteredFacturas.slice(0, 500);

  const handleImport = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setImporting(true); setNotice(null);
    try {
      const body = new FormData(); body.append('file', file);
      const response = await apiClient.post('/api/imports/zeuz/excel', body, { headers: { 'Content-Type': 'multipart/form-data' } });
      await loadFacturas();
      setNotice({ type: 'success', text: `Reporte procesado. ${response.data.ventasImportadas || 0} ventas nuevas y sus facturas fueron sincronizadas.` });
    } catch (error: any) {
      const message = error?.response?.data?.message || error?.response?.data?.error || 'No fue posible importar el reporte de Zeuz.';
      setNotice({ type: 'error', text: message });
    } finally {
      setImporting(false);
      if (fileInput.current) fileInput.current.value = '';
    }
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    try {
      if (editingFactura?.id) await apiClient.put(`/api/facturas/${editingFactura.id}`, formData);
      else await apiClient.post('/api/facturas', formData);
      await loadFacturas(); closeEditor();
    } catch {
      setNotice({ type: 'error', text: 'No fue posible guardar la factura.' });
    }
  };

  const openNew = () => { setEditingFactura(null); setFormData(emptyInvoice); setIsModalOpen(true); };
  const openEdit = (factura: Factura) => { setEditingFactura(factura); setFormData(factura); setIsModalOpen(true); };
  const closeEditor = () => { setIsModalOpen(false); setEditingFactura(null); setFormData(emptyInvoice); };

  const openDetail = async (factura: Factura) => {
    if (!factura.id) return;
    try { setDetailFactura((await apiClient.get(`/api/facturas/${factura.id}`)).data); }
    catch { setNotice({ type: 'error', text: 'No fue posible cargar el detalle de la factura.' }); }
  };

  const handleDelete = async (id: number) => {
    if (!window.confirm('¿Está seguro de eliminar esta factura?')) return;
    try { await apiClient.delete(`/api/facturas/${id}`); await loadFacturas(); }
    catch { setNotice({ type: 'error', text: 'No fue posible eliminar la factura.' }); }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900 flex items-center gap-3"><Receipt className="w-8 h-8 text-sky-600" />Facturación</h1>
          <p className="text-slate-500 mt-1">Facturas de ventas agrupadas desde el reporte Fare Basis de Zeuz.</p>
        </div>
        <div className="flex flex-wrap gap-3">
          <input ref={fileInput} type="file" accept=".xlsx,.xlsm" onChange={handleImport} className="hidden" />
          <Button variant="secondary" onClick={() => fileInput.current?.click()} disabled={importing}>
            {importing ? <FileSpreadsheet className="w-4 h-4 mr-2 animate-pulse" /> : <Upload className="w-4 h-4 mr-2" />}
            {importing ? 'Procesando reporte...' : 'Importar reporte Zeuz'}
          </Button>
          <Button onClick={openNew}><Plus className="w-4 h-4 mr-2" />Nueva factura manual</Button>
        </div>
      </div>

      {notice && <div className={`flex items-center gap-2 rounded-xl border p-4 ${notice.type === 'success' ? 'bg-emerald-50 border-emerald-200 text-emerald-800' : 'bg-red-50 border-red-200 text-red-800'}`}>
        {notice.type === 'success' ? <CheckCircle2 className="w-5 h-5" /> : <AlertCircle className="w-5 h-5" />}{notice.text}
      </div>}

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="card"><p className="text-sm text-slate-500">Facturación total</p><p className="text-2xl font-bold mt-1">{money(summary.total)}</p><p className="text-xs text-slate-400 mt-1">{summary.cantidad} facturas registradas</p></div>
        <div className="card"><p className="text-sm text-slate-500">Pendiente y vencida</p><p className="text-2xl font-bold text-amber-700 mt-1">{money(summary.pendiente)}</p></div>
        <div className="card"><p className="text-sm text-slate-500">Pagada</p><p className="text-2xl font-bold text-emerald-700 mt-1">{money(summary.pagada)}</p></div>
      </div>

      <div className="card grid grid-cols-1 md:grid-cols-[1fr_220px] gap-4">
        <div className="relative"><Search className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 w-5 h-5" /><Input placeholder="Buscar por factura, número Zeuz, cliente o vendedor..." value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} className="pl-10" /></div>
        <select className="input" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}><option value="TODAS">Todos los estados</option><option value="PENDIENTE">Pendientes</option><option value="PAGADA">Pagadas</option><option value="VENCIDA">Vencidas</option><option value="ANULADA">Anuladas</option></select>
      </div>

      <div className="card overflow-x-auto">
        <table className="table min-w-[1100px]">
          <thead><tr><th>Número de factura</th><th>Número Zeuz</th><th>Cliente</th><th>Vendedor / agencia</th><th>Emisión</th><th>Tiquetes</th><th>Estado</th><th>Total tarifa</th><th>Acciones</th></tr></thead>
          <tbody>{visibleFacturas.map((factura) => <tr key={factura.id}>
            <td className="font-semibold text-slate-900">{factura.numero}</td><td>{factura.numeroZeuz || '—'}</td><td>{factura.clienteNombre || `Cliente ${factura.clienteId || '—'}`}</td><td>{factura.vendedor || '—'}</td><td>{date(factura.fechaEmision)}</td><td>{factura.cantidadItems || 0}</td>
            <td><span className={`px-2 py-1 rounded-full text-xs font-medium ${statusClass(factura.estado)}`}>{factura.estado}</span></td><td className="font-semibold">{money(factura.total, factura.moneda)}</td>
            <td><div className="flex gap-2"><Button variant="secondary" onClick={() => openDetail(factura)} title="Ver detalle"><Eye className="w-4 h-4" /></Button><Button variant="secondary" onClick={() => openEdit(factura)} title="Editar"><Edit className="w-4 h-4" /></Button><Button variant="danger" onClick={() => handleDelete(factura.id!)} title="Eliminar"><Trash2 className="w-4 h-4" /></Button></div></td>
          </tr>)}</tbody>
        </table>
        {!loading && filteredFacturas.length === 0 && <p className="text-center text-slate-500 py-10">No hay facturas para los filtros seleccionados.</p>}
        {loading && <p className="text-center text-slate-500 py-10">Cargando facturas...</p>}
        {!loading && summary.cantidad > visibleFacturas.length && <p className="text-center text-slate-500 py-4">Mostrando las 500 facturas más recientes de {summary.cantidad}. Usa el buscador y los filtros sobre este listado.</p>}
      </div>

      <Modal isOpen={Boolean(detailFactura)} onClose={() => setDetailFactura(null)} title={`Factura ${detailFactura?.numero || ''}`}>
        {detailFactura && <div className="space-y-5">
          <div className="grid grid-cols-2 gap-4 text-sm"><div><p className="text-slate-500">Número Zeuz</p><p className="font-semibold">{detailFactura.numeroZeuz || '—'}</p></div><div><p className="text-slate-500">Cliente</p><p className="font-semibold">{detailFactura.clienteNombre || '—'}</p></div><div><p className="text-slate-500">Fecha de emisión</p><p className="font-semibold">{date(detailFactura.fechaEmision)}</p></div><div><p className="text-slate-500">Total tarifa</p><p className="font-semibold">{money(detailFactura.total, detailFactura.moneda)}</p></div></div>
          <div className="overflow-x-auto max-h-96"><table className="table min-w-[850px]"><thead><tr><th>Tiquete</th><th>Pasajero</th><th>Ruta</th><th>Aerolínea</th><th>Localizador</th><th>Viaje</th><th>Tarifa</th></tr></thead><tbody>{(detailFactura.detalles || []).map(item => <tr key={item.id || item.documento}><td>{item.documento}</td><td>{item.pasajero || '—'}</td><td>{item.ruta || '—'}</td><td>{item.aerolinea || '—'}</td><td>{item.localizador || '—'}</td><td>{date(item.fechaViaje)}</td><td className="font-semibold">{money(item.tarifa, item.moneda)}</td></tr>)}</tbody></table>{!detailFactura.detalles?.length && <p className="text-center text-slate-500 py-6">Esta factura no tiene detalles importados.</p>}</div>
        </div>}
      </Modal>

      <Modal isOpen={isModalOpen} onClose={closeEditor} title={editingFactura?.id ? 'Editar factura' : 'Nueva factura'}>
        <form onSubmit={handleSubmit}><div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Input label="Número de factura" value={formData.numero} onChange={(e) => setFormData({ ...formData, numero: e.target.value })} required /><Input label="Número Zeuz" value={formData.numeroZeuz || ''} onChange={(e) => setFormData({ ...formData, numeroZeuz: e.target.value })} />
          <div><label className="block text-sm font-medium text-gray-700 mb-1">Estado</label><select className="input" value={formData.estado} onChange={(e) => setFormData({ ...formData, estado: e.target.value })}><option value="PENDIENTE">Pendiente</option><option value="PAGADA">Pagada</option><option value="VENCIDA">Vencida</option><option value="ANULADA">Anulada</option></select></div>
          <Input label="ID Cliente" type="number" value={formData.clienteId} onChange={(e) => setFormData({ ...formData, clienteId: Number(e.target.value) })} /><Input label="Fecha de emisión" type="date" value={formData.fechaEmision} onChange={(e) => setFormData({ ...formData, fechaEmision: e.target.value })} /><Input label="Fecha de vencimiento" type="date" value={formData.fechaVencimiento} onChange={(e) => setFormData({ ...formData, fechaVencimiento: e.target.value })} />
          <Input label="Subtotal" type="number" value={formData.subtotal} onChange={(e) => setFormData({ ...formData, subtotal: Number(e.target.value) })} /><Input label="Impuesto" type="number" value={formData.impuesto} onChange={(e) => setFormData({ ...formData, impuesto: Number(e.target.value) })} /><Input label="Total" type="number" value={formData.total} onChange={(e) => setFormData({ ...formData, total: Number(e.target.value) })} />
          <div><label className="block text-sm font-medium text-gray-700 mb-1">Moneda</label><select className="input" value={formData.moneda} onChange={(e) => setFormData({ ...formData, moneda: e.target.value })}><option value="COP">COP</option><option value="USD">USD</option><option value="EUR">EUR</option></select></div>
        </div><div className="mt-4"><label className="block text-sm font-medium text-gray-700 mb-1">Notas</label><textarea className="input" rows={3} value={formData.notas} onChange={(e) => setFormData({ ...formData, notas: e.target.value })} /></div><div className="mt-6 flex justify-end gap-3"><Button type="button" variant="secondary" onClick={closeEditor}>Cancelar</Button><Button type="submit">Guardar</Button></div></form>
      </Modal>
    </div>
  );
};

export default FacturasPage;
