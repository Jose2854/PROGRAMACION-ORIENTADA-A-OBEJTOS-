import { useEffect, useState } from 'react';
import { BarChart3, DollarSign, Plane, RefreshCw, Upload, Users } from 'lucide-react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import apiClient from '../services/api';
import Button from '../components/Button';
import Input from '../components/Input';
import { useAuth } from '../context/AuthContext';
import { TourismMetric as Metric, TourismReport } from '../types/tourism';

interface TenantOption { id: number; name: string; slug: string; status: string; }

interface ImportResult {
  filasLeidas: number;
  ventasImportadas: number;
  duplicadosOmitidos: number;
  filasConError: number;
  totalImportado: number;
  advertencias: string[];
}

const isoDate = (date: Date) => date.toISOString().slice(0, 10);

const Ranking = ({ title, data }: { title: string; data: Record<string, Metric> }) => (
  <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
    <h2 className="mb-4 font-bold text-slate-900">{title}</h2>
    <div className="space-y-3">
      {Object.entries(data).slice(0, 8).map(([label, metric]) => (
        <div key={label} className="flex items-center justify-between gap-4 border-b border-slate-100 pb-3 last:border-0">
          <div className="min-w-0">
            <p className="truncate text-sm font-semibold text-slate-700" title={label}>{label}</p>
            <p className="text-xs text-slate-400">{metric.ventas} ventas · {metric.pasajeros} pasajeros</p>
          </div>
          <div className="shrink-0 text-right">
            <p className="text-sm font-bold text-slate-900">{Number(metric.ingreso).toLocaleString('es-CO')}</p>
            <p className="text-xs font-medium text-emerald-600">Margen {Number(metric.margen).toLocaleString('es-CO')}</p>
          </div>
        </div>
      ))}
      {Object.keys(data).length === 0 && <p className="py-6 text-center text-sm text-slate-400">Sin ventas cerradas en este periodo.</p>}
    </div>
  </section>
);

export default function TourismReports() {
  const { role, tenantId } = useAuth();
  const isSuperAdmin = role === 'SUPER_ADMIN';
  const today = new Date();
  const initialFrom = new Date(today);
  initialFrom.setDate(today.getDate() - 29);
  const [from, setFrom] = useState(isoDate(initialFrom));
  const [to, setTo] = useState(isoDate(today));
  const [report, setReport] = useState<TourismReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState<ImportResult | null>(null);
  const [tenants, setTenants] = useState<TenantOption[]>([]);
  const [selectedTenantId, setSelectedTenantId] = useState<string>(() => localStorage.getItem('tourismTenantId') || '');

  const tenantHeaders = selectedTenantId ? { 'X-Tenant-Id': selectedTenantId } : undefined;

  const loadReport = async () => {
    setLoading(true);
    setError(null);
    try {
      if (isSuperAdmin && !selectedTenantId) {
        setReport(null);
        return;
      }
      const response = await apiClient.get<TourismReport>('/api/reports/tourism', {
        params: { from, to }, headers: tenantHeaders,
      });
      setReport(response.data);
    } catch (requestError) {
      console.error('Error loading tourism report:', requestError);
      setError('No fue posible cargar el reporte turístico.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!isSuperAdmin) return;
    apiClient.get<TenantOption[]>('/api/v1/superadmin/tenants').then(response => {
      const available = response.data.filter(item => item.status !== 'CANCELLED');
      setTenants(available);
      if (!selectedTenantId && available.length === 1) setSelectedTenantId(String(available[0].id));
    }).catch(error => console.error('Error loading tenants:', error));
  }, [isSuperAdmin]);

  useEffect(() => { loadReport(); }, [selectedTenantId, tenantId]);

  useEffect(() => {
    if (selectedTenantId) localStorage.setItem('tourismTenantId', selectedTenantId);
  }, [selectedTenantId]);

  const importZeuz = async (file?: File) => {
    if (!file) return;
    setImporting(true);
    setImportResult(null);
    try {
      const form = new FormData();
      form.append('file', file);
      if (isSuperAdmin && !selectedTenantId) {
        setError('Selecciona primero la empresa del reporte.');
        return;
      }
      const accessToken = localStorage.getItem('accessToken');
      const headers = new Headers();
      if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`);
      if (selectedTenantId) headers.set('X-Tenant-Id', selectedTenantId);

      const response = await fetch('/api/imports/zeuz/excel', {
        method: 'POST',
        headers,
        body: form,
      });
      const responseData = await response.json().catch(() => null);
      if (!response.ok) {
        throw { response: { data: responseData } };
      }
      setImportResult(responseData as ImportResult);
      await loadReport();
    } catch (requestError) {
      console.error('Error importing Zeuz report:', requestError);
      const responseData = (requestError as { response?: { data?: { message?: string } | string } }).response?.data;
      const detail = typeof responseData === 'string' ? responseData : responseData?.message;
      setError(detail || 'No fue posible importar el archivo. Verifica que corresponda a un reporte Excel de Zeuz.');
    } finally {
      setImporting(false);
    }
  };

  const dailyData = Object.entries(report?.ventasPorDia || {}).map(([date, metric]) => ({ date: date.slice(5), ingreso: metric.ingreso, margen: metric.margen }));
  const cards = report ? [
    { label: 'Ventas cerradas', value: report.totals.ventas.toLocaleString('es-CO'), icon: BarChart3, color: 'text-blue-600', bg: 'bg-blue-50' },
    { label: 'Pasajeros', value: report.totals.pasajeros.toLocaleString('es-CO'), icon: Users, color: 'text-violet-600', bg: 'bg-violet-50' },
    { label: 'Ingreso', value: Number(report.totals.ingreso).toLocaleString('es-CO'), icon: DollarSign, color: 'text-emerald-600', bg: 'bg-emerald-50' },
    { label: 'Margen con costo informado', value: Number(report.totals.margen).toLocaleString('es-CO'), icon: Plane, color: 'text-amber-600', bg: 'bg-amber-50' },
  ] : [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col justify-between gap-4 lg:flex-row lg:items-end">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Inteligencia turística</h1>
          <p className="mt-1 text-sm text-slate-500">Ventas diarias y rentabilidad por cliente, producto y aerolínea.</p>
          <label className="mt-3 inline-flex cursor-pointer items-center rounded-lg bg-slate-900 px-4 py-2 text-sm font-semibold text-white hover:bg-slate-800">
            <Upload className="mr-2 h-4 w-4" />{importing ? 'Importando reporte…' : 'Importar Excel de Zeuz'}
            <input
              type="file"
              accept=".xlsx,.xlsm"
              className="hidden"
              disabled={importing}
              onChange={event => {
                const file = event.target.files?.[0];
                event.target.value = '';
                importZeuz(file);
              }}
            />
          </label>
        </div>
        <div className="flex flex-wrap items-end gap-3 rounded-2xl border border-slate-200 bg-white p-3">
          <Input label="Desde" type="date" value={from} onChange={event => setFrom(event.target.value)} className="min-w-36" />
          <Input label="Hasta" type="date" value={to} onChange={event => setTo(event.target.value)} className="min-w-36" />
          <Button onClick={loadReport} disabled={loading}><RefreshCw className={`mr-2 h-4 w-4 ${loading ? 'animate-spin' : ''}`} />Actualizar</Button>
        </div>
      </div>

      {isSuperAdmin && (
        <div className="rounded-2xl border border-blue-200 bg-blue-50 p-4">
          <label className="mb-1 block text-sm font-semibold text-blue-900">Empresa para consultar e importar</label>
          <select value={selectedTenantId} onChange={event => setSelectedTenantId(event.target.value)} className="w-full max-w-md rounded-lg border border-blue-200 bg-white px-3 py-2 text-sm text-slate-800">
            <option value="">Selecciona una empresa</option>
            {tenants.map(item => <option key={item.id} value={item.id}>{item.name} ({item.slug})</option>)}
          </select>
          <p className="mt-1 text-xs text-blue-700">Las ventas del Excel quedarán únicamente en la empresa seleccionada.</p>
        </div>
      )}

      {error && <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}
      {importResult && (
        <div className="rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800">
          <strong>Importación terminada:</strong> {importResult.ventasImportadas.toLocaleString('es-CO')} ventas nuevas,
          {' '}{importResult.duplicadosOmitidos.toLocaleString('es-CO')} duplicados omitidos y {importResult.filasConError} filas con error.
          {importResult.advertencias.length > 0 && <p className="mt-1 text-xs">Advertencias: {importResult.advertencias.slice(0, 3).join(' · ')}</p>}
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {cards.map(card => <div key={card.label} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><div className={`mb-3 inline-flex rounded-xl p-2 ${card.bg}`}><card.icon className={`h-5 w-5 ${card.color}`} /></div><p className="text-xs font-semibold uppercase tracking-wide text-slate-400">{card.label}</p><p className="mt-1 text-2xl font-black text-slate-900">{card.value}</p></div>)}
      </div>

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="font-bold text-slate-900">Ventas día a día</h2>
        <p className="mb-4 text-xs text-slate-400">Ingreso y margen de las ventas cerradas</p>
        <ResponsiveContainer width="100%" height={300}>
          <BarChart data={dailyData}><CartesianGrid strokeDasharray="3 3" vertical={false} /><XAxis dataKey="date" /><YAxis /><Tooltip formatter={value => Number(value).toLocaleString('es-CO')} /><Bar dataKey="ingreso" name="Ingreso" fill="#2563eb" radius={[5, 5, 0, 0]} /><Bar dataKey="margen" name="Margen" fill="#10b981" radius={[5, 5, 0, 0]} /></BarChart>
        </ResponsiveContainer>
      </section>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <Ranking title="Ventas por cliente" data={report?.ventasPorCliente || {}} />
        <Ranking title="Ventas por tipo de producto" data={report?.ventasPorTipoProducto || {}} />
        <Ranking title="Ventas por aerolínea" data={report?.ventasPorAerolinea || {}} />
        <Ranking title="Ventas por hotel" data={report?.ventasPorHotel || {}} />
        <Ranking title="Ventas por destino" data={report?.ventasPorDestino || {}} />
        <Ranking title="Ventas por canal" data={report?.ventasPorCanal || {}} />
        <Ranking title="Totales por moneda" data={report?.ventasPorMoneda || {}} />
      </div>
    </div>
  );
}
