/*
 * CRM SaaS - Copyright (c) 2024-2026 Hector Andres Ladino
 * Licensed under MIT License. See LICENSE file for details.
 */
import { useState, useEffect } from 'react';
import { Plus, Edit, Trash2, Search, Plane, Users, DollarSign, BarChart3, ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';
import apiClient from '../services/api';
import { Venta } from '../types';
import Button from '../components/Button';
import Input from '../components/Input';
import Modal from '../components/Modal';
import { TourismReport } from '../types/tourism';

const isoDate = (date: Date) => date.toISOString().slice(0, 10);

const Ventas = () => {
  const [ventas, setVentas] = useState<Venta[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingVenta, setEditingVenta] = useState<Venta | null>(null);
  const [searchTerm, setSearchTerm] = useState('');
  const today = new Date();
  const [reportFrom, setReportFrom] = useState(`${today.getFullYear()}-01-01`);
  const [reportTo, setReportTo] = useState(isoDate(today));
  const [report, setReport] = useState<TourismReport | null>(null);
  const [reportError, setReportError] = useState(false);
  const [formData, setFormData] = useState<Venta>({
    clienteId: 0,
    codigo: '',
    descripcion: '',
    monto: 0,
    descuento: 0,
    impuesto: 0,
    total: 0,
    comision: 0,
    vendedor: '',
    notas: '',
    estado: 'PENDIENTE',
    metodoPago: 'TRANSFERENCIA',
    tipoProductoTuristico: 'PAQUETE',
    moneda: 'COP',
    numeroPasajeros: 1,
    costo: 0,
  });
  
  useEffect(() => {
    loadVentas();
    loadReport();
  }, []);

  const loadReport = async () => {
    setReportError(false);
    try {
      const response = await apiClient.get<TourismReport>('/api/reports/tourism', { params: { from: reportFrom, to: reportTo } });
      setReport(response.data);
    } catch (error) {
      console.error('Error loading tourism sales report:', error);
      setReportError(true);
    }
  };
  
  const loadVentas = async () => {
    try {
      const response = await apiClient.get('/api/ventas', { params: { limit: 500 } });
      setVentas(response.data);
    } catch (error) {
      console.error('Error loading ventas:', error);
    }
  };
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      if (editingVenta?.id) {
        await apiClient.put(`/api/ventas/${editingVenta.id}`, formData);
      } else {
        await apiClient.post('/api/ventas', formData);
      }
      loadVentas();
      setIsModalOpen(false);
      setEditingVenta(null);
    } catch (error) {
      console.error('Error saving venta:', error);
    }
  };
  
  const handleEdit = (venta: Venta) => {
    setEditingVenta(venta);
    setFormData({ ...venta, clienteId: venta.clienteId || venta.cliente?.id || 0 });
    setIsModalOpen(true);
  };
  
  const handleDelete = async (id: number) => {
    if (window.confirm('¿Está seguro de eliminar esta venta?')) {
      try {
        await apiClient.delete(`/api/ventas/${id}`);
        loadVentas();
      } catch (error) {
        console.error('Error deleting venta:', error);
      }
    }
  };
  
  const filteredVentas = ventas.filter(
    (venta) =>
      venta.codigo.toLowerCase().includes(searchTerm.toLowerCase()) ||
      venta.descripcion.toLowerCase().includes(searchTerm.toLowerCase()) ||
      venta.destino?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      venta.aerolinea?.toLowerCase().includes(searchTerm.toLowerCase()) ||
      venta.vendedor?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const topAirlines = Object.entries(report?.ventasPorAerolinea || {}).slice(0, 5);
  const money = (value: number) => new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(value || 0);
  
  return (
    <div>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Ventas turísticas</h1>
          <p className="mt-1 text-sm text-slate-500">Reservas, aerolíneas y rentabilidad por cliente.</p>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>
          <Plus className="w-4 h-4 mr-2" />
          Nueva Venta
        </Button>
      </div>

      <section className="card mb-6">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Informe turístico de ventas</h2>
            <p className="text-sm text-slate-500">Resumen consolidado de Zeuz y de las ventas registradas en el CRM.</p>
          </div>
          <div className="flex flex-wrap items-end gap-2">
            <Input label="Desde" type="date" value={reportFrom} onChange={(e) => setReportFrom(e.target.value)} />
            <Input label="Hasta" type="date" value={reportTo} onChange={(e) => setReportTo(e.target.value)} />
            <Button type="button" onClick={loadReport}>Actualizar</Button>
            <Link to="/turismo" className="inline-flex h-10 items-center gap-2 rounded-lg border border-slate-300 px-3 text-sm font-semibold text-slate-700 hover:bg-slate-50">
              Ver informe completo <ExternalLink className="h-4 w-4" />
            </Link>
          </div>
        </div>
        {reportError && <p className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">No fue posible actualizar el informe turístico.</p>}
        {report && <>
          <div className="mt-5 grid grid-cols-2 gap-3 lg:grid-cols-4">
            {[
              { label: 'Ventas cerradas', value: report.totals.ventas.toLocaleString('es-CO'), icon: BarChart3, color: 'text-blue-600 bg-blue-50' },
              { label: 'Pasajeros', value: report.totals.pasajeros.toLocaleString('es-CO'), icon: Users, color: 'text-violet-600 bg-violet-50' },
              { label: 'Ingreso', value: money(report.totals.ingreso), icon: DollarSign, color: 'text-emerald-600 bg-emerald-50' },
              { label: 'Aerolíneas', value: Object.keys(report.ventasPorAerolinea).length.toLocaleString('es-CO'), icon: Plane, color: 'text-amber-600 bg-amber-50' },
            ].map(item => <div key={item.label} className="rounded-xl border border-slate-200 p-4"><div className={`mb-3 inline-flex rounded-lg p-2 ${item.color}`}><item.icon className="h-4 w-4" /></div><p className="text-xl font-bold text-slate-900">{item.value}</p><p className="text-xs text-slate-500">{item.label}</p></div>)}
          </div>
          <div className="mt-5">
            <h3 className="mb-3 text-sm font-bold text-slate-700">Principales aerolíneas</h3>
            <div className="grid gap-2 md:grid-cols-5">
              {topAirlines.map(([name, metric]) => <div key={name} className="rounded-lg bg-slate-50 p-3"><p className="truncate text-sm font-semibold text-slate-800" title={name}>{name}</p><p className="mt-1 text-xs text-slate-500">{metric.ventas.toLocaleString('es-CO')} ventas</p><p className="text-xs font-semibold text-blue-700">{money(metric.ingreso)}</p></div>)}
            </div>
          </div>
        </>}
      </section>
      
      <div className="card mb-6">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
          <Input
            placeholder="Buscar por código, destino, aerolínea o vendedor..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-10"
          />
        </div>
      </div>
      
      <div className="card">
        <table className="table">
          <thead>
            <tr>
              <th>Código</th>
              <th>Descripción</th>
              <th>Producto</th>
              <th>Destino / aerolínea</th>
              <th>Monto</th>
              <th>Margen</th>
              <th>Tarifa</th>
              <th>Vendedor</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            {filteredVentas.slice(0, 100).map((venta) => (
              <tr key={venta.id}>
                <td>{venta.codigo}</td>
                <td>{venta.descripcion}</td>
                <td>{venta.tipoProductoTuristico || 'OTRO'}</td>
                <td>
                  <div className="font-medium text-slate-800">{venta.destino || 'Sin destino'}</div>
                  <div className="text-xs text-slate-500">{venta.aerolinea || venta.proveedor || venta.hotel || 'Sin aerolínea'}</div>
                </td>
                <td>${venta.monto}</td>
                <td>{venta.costo == null ? 'Sin costo' : money(Number(venta.tarifa ?? venta.total ?? 0) - Number(venta.costo))}</td>
                <td>{money(Number(venta.tarifa ?? venta.total ?? 0))}</td>
                <td>{venta.vendedor}</td>
                <td>
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                    venta.estado === 'CERRADA' ? 'bg-green-100 text-green-800' : 'bg-yellow-100 text-yellow-800'
                  }`}>
                    {venta.estado}
                  </span>
                </td>
                <td>
                  <div className="flex space-x-2">
                    <Button variant="secondary" size="sm" onClick={() => handleEdit(venta)}>
                      <Edit className="w-4 h-4" />
                    </Button>
                    <Button variant="danger" size="sm" onClick={() => handleDelete(venta.id!)}>
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {filteredVentas.length === 0 && (
          <p className="text-center text-gray-500 py-8">No hay ventas registradas</p>
        )}
        {filteredVentas.length > 100 && <p className="border-t border-slate-100 py-3 text-center text-xs text-slate-500">Mostrando 100 de {filteredVentas.length.toLocaleString('es-CO')} ventas. Usa el buscador para acotar los resultados.</p>}
      </div>
      
      <Modal isOpen={isModalOpen} onClose={() => { setIsModalOpen(false); setEditingVenta(null); }} title={editingVenta?.id ? 'Editar Venta' : 'Nueva Venta'}>
        <form onSubmit={handleSubmit}>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input label="Código" value={formData.codigo} onChange={(e) => setFormData({ ...formData, codigo: e.target.value })} required />
            <Input label="ID Cliente" type="number" value={formData.clienteId} onChange={(e) => setFormData({ ...formData, clienteId: Number(e.target.value) })} required />
            <Input label="Descripción" value={formData.descripcion} onChange={(e) => setFormData({ ...formData, descripcion: e.target.value })} required />
            <Input label="Monto" type="number" value={formData.monto} onChange={(e) => setFormData({ ...formData, monto: Number(e.target.value) })} required />
            <Input label="Descuento" type="number" value={formData.descuento} onChange={(e) => setFormData({ ...formData, descuento: Number(e.target.value) })} />
            <Input label="Impuesto" type="number" value={formData.impuesto} onChange={(e) => setFormData({ ...formData, impuesto: Number(e.target.value) })} />
            <Input label="Vendedor" value={formData.vendedor} onChange={(e) => setFormData({ ...formData, vendedor: e.target.value })} />
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Producto turístico</label>
              <select className="input" value={formData.tipoProductoTuristico} onChange={(e) => setFormData({ ...formData, tipoProductoTuristico: e.target.value as Venta['tipoProductoTuristico'] })}>
                {['VUELO', 'HOTEL', 'PAQUETE', 'CRUCERO', 'TRASLADO', 'EXCURSION', 'SEGURO', 'OTRO'].map(tipo => <option key={tipo} value={tipo}>{tipo}</option>)}
              </select>
            </div>
            <Input label="Origen" value={formData.origen || ''} onChange={(e) => setFormData({ ...formData, origen: e.target.value })} />
            <Input label="Destino" value={formData.destino || ''} onChange={(e) => setFormData({ ...formData, destino: e.target.value })} />
            <Input label="Aerolínea" value={formData.aerolinea || ''} onChange={(e) => setFormData({ ...formData, aerolinea: e.target.value })} />
            <Input label="Hotel" value={formData.hotel || ''} onChange={(e) => setFormData({ ...formData, hotel: e.target.value })} />
            <Input label="Localizador / reserva" value={formData.localizador || ''} onChange={(e) => setFormData({ ...formData, localizador: e.target.value })} />
            <Input label="Fecha inicio del viaje" type="date" value={formData.fechaViajeInicio || ''} onChange={(e) => setFormData({ ...formData, fechaViajeInicio: e.target.value })} />
            <Input label="Fecha fin del viaje" type="date" value={formData.fechaViajeFin || ''} onChange={(e) => setFormData({ ...formData, fechaViajeFin: e.target.value })} />
            <Input label="Pasajeros" type="number" min="1" value={formData.numeroPasajeros || 1} onChange={(e) => setFormData({ ...formData, numeroPasajeros: Number(e.target.value) })} />
            <Input label="Costo" type="number" value={formData.costo || 0} onChange={(e) => setFormData({ ...formData, costo: Number(e.target.value) })} />
            <Input label="Moneda" maxLength={3} value={formData.moneda || 'COP'} onChange={(e) => setFormData({ ...formData, moneda: e.target.value.toUpperCase() })} />
            <Input label="Canal de venta" placeholder="Web, WhatsApp, oficina..." value={formData.canalVenta || ''} onChange={(e) => setFormData({ ...formData, canalVenta: e.target.value })} />
            <Input label="ID en back office" value={formData.externalId || ''} onChange={(e) => setFormData({ ...formData, externalId: e.target.value })} />
            <Input label="Sistema de origen" value={formData.backofficeOrigen || ''} onChange={(e) => setFormData({ ...formData, backofficeOrigen: e.target.value })} />
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Estado</label>
              <select className="input" value={formData.estado} onChange={(e) => setFormData({ ...formData, estado: e.target.value as any })}>
                <option value="PENDIENTE">Pendiente</option>
                <option value="EN_PROCESO">En Proceso</option>
                <option value="CERRADA">Cerrada</option>
                <option value="CANCELADA">Cancelada</option>
              </select>
            </div>
          </div>
          <div className="mt-4">
            <label className="block text-sm font-medium text-gray-700 mb-1">Notas</label>
            <textarea className="input" rows={3} value={formData.notas} onChange={(e) => setFormData({ ...formData, notas: e.target.value })} />
          </div>
          <div className="mt-6 flex justify-end space-x-3">
            <Button type="button" variant="secondary" onClick={() => { setIsModalOpen(false); setEditingVenta(null); }}>Cancelar</Button>
            <Button type="submit">Guardar</Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

export default Ventas;
