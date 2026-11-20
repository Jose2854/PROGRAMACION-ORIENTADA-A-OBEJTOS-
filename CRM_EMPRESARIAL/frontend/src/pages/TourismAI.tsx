import { useEffect, useMemo, useState } from 'react';
import { BrainCircuit, CalendarDays, Plane, RefreshCw, Sparkles, TrendingUp, Users } from 'lucide-react';
import apiClient from '../services/api';
import { TourismAiClient, TourismAiInsights } from '../types/tourism-ai';

const isoDate = (date: Date) => date.toISOString().slice(0, 10);
const money = (value: number) => new Intl.NumberFormat('es-CO', {
  style: 'currency', currency: 'COP', maximumFractionDigits: 0,
}).format(value || 0);

const scoreColor = (score: number) => score >= 80
  ? 'bg-emerald-500' : score >= 60 ? 'bg-blue-500' : score >= 40 ? 'bg-amber-500' : 'bg-slate-400';

const levelLabel: Record<TourismAiClient['level'], string> = {
  MUY_ALTO: 'Muy alto', ALTO: 'Alto', MEDIO: 'Medio', POR_ACTIVAR: 'Por activar',
};

export default function TourismAI() {
  const today = useMemo(() => new Date(), []);
  const [from, setFrom] = useState(`${today.getFullYear()}-01-01`);
  const [to, setTo] = useState(isoDate(today));
  const [data, setData] = useState<TourismAiInsights | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = async (selectedFrom = from, selectedTo = to) => {
    if (!selectedFrom || !selectedTo) {
      setError('Selecciona las dos fechas para realizar el análisis.');
      return;
    }
    if (selectedFrom > selectedTo) {
      setError('La fecha inicial no puede ser posterior a la fecha final.');
      return;
    }
    setLoading(true);
    setError('');
    try {
      const response = await apiClient.get<TourismAiInsights>('/api/reports/tourism/ai-insights', { params: { from: selectedFrom, to: selectedTo } });
      setData(response.data);
    } catch (requestError) {
      console.error('Error loading tourism AI insights:', requestError);
      setError('No fue posible analizar las ventas. Verifica la empresa seleccionada y vuelve a intentar.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, []);

  return (
    <div className="space-y-6">
      <section className="overflow-hidden rounded-3xl bg-gradient-to-br from-slate-950 via-blue-950 to-sky-800 p-6 text-white shadow-xl">
        <div className="flex flex-col justify-between gap-5 lg:flex-row lg:items-center">
          <div className="flex gap-4">
            <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-white/10 ring-1 ring-white/20">
              <BrainCircuit size={30} />
            </div>
            <div>
              <div className="mb-1 flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-sky-200"><Sparkles size={14} /> IA comercial turística</div>
              <h1 className="text-2xl font-black md:text-3xl">Clientes y aerolíneas con mayor valor</h1>
              <p className="mt-1 max-w-2xl text-sm text-blue-100/80">Análisis explicable de tarifa vendida, frecuencia, recencia y diversidad de compra.</p>
            </div>
          </div>
          <div className="flex flex-wrap items-end gap-3 rounded-2xl bg-white/10 p-3 ring-1 ring-white/15">
            <label className="text-xs font-semibold text-blue-100">Desde<input type="date" value={from} onChange={e => setFrom(e.target.value)} className="mt-1 block rounded-lg border-0 bg-white px-3 py-2 text-sm text-slate-900" /></label>
            <label className="text-xs font-semibold text-blue-100">Hasta<input type="date" value={to} onChange={e => setTo(e.target.value)} className="mt-1 block rounded-lg border-0 bg-white px-3 py-2 text-sm text-slate-900" /></label>
            <button onClick={() => load()} disabled={loading} className="inline-flex h-10 items-center gap-2 rounded-lg bg-sky-400 px-4 text-sm font-bold text-slate-950 hover:bg-sky-300 disabled:opacity-60"><RefreshCw size={16} className={loading ? 'animate-spin' : ''} /> Analizar</button>
          </div>
        </div>
      </section>

      {error && <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">{error}</div>}
      {loading && <div className="rounded-2xl border border-slate-200 bg-white p-12 text-center text-slate-500">Analizando comportamiento comercial…</div>}

      {!loading && data && <>
        {!data.clients.length && data.availableFrom && data.availableTo && <div className="flex flex-col justify-between gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900 sm:flex-row sm:items-center">
          <div><strong>No existen ventas cerradas entre {from} y {to}.</strong><p className="mt-1 text-xs text-amber-800">El reporte cargado contiene información desde {data.availableFrom} hasta {data.availableTo}.</p></div>
          <button onClick={() => { setFrom(data.availableFrom!); setTo(data.availableTo!); load(data.availableFrom!, data.availableTo!); }} className="shrink-0 rounded-lg bg-amber-900 px-4 py-2 text-xs font-bold text-white hover:bg-amber-800">Usar periodo disponible</button>
        </div>}
        <section className="grid gap-4 md:grid-cols-3">
          <SummaryCard icon={Users} eyebrow="Cliente que más vende" title={data.summary.topClient} value={money(data.summary.topClientRevenue)} color="emerald" />
          <SummaryCard icon={Plane} eyebrow="Aerolínea líder" title={data.summary.topAirline} value={money(data.summary.topAirlineRevenue)} color="sky" />
          <SummaryCard icon={TrendingUp} eyebrow="Mayor potencial" title={data.summary.highestPotentialClient} value={`${data.summary.highestPotentialScore}/100`} color="violet" />
        </section>

        <section className="grid gap-4 sm:grid-cols-3">
          <MiniMetric label="Clientes de alto potencial" value={data.summary.highPotentialClients.toLocaleString('es-CO')} detail="Puntaje igual o superior a 60" />
          <MiniMetric label="Clientes en riesgo" value={data.summary.atRiskClients.toLocaleString('es-CO')} detail="Requieren reactivación comercial" tone="amber" />
          <MiniMetric label="Concentración aerolínea líder" value={`${data.summary.topAirlineConcentration.toFixed(1)}%`} detail="Participación sobre la tarifa vendida" tone={data.summary.topAirlineConcentration > 45 ? 'amber' : 'blue'} />
        </section>

        <section className="grid gap-6 xl:grid-cols-[1.45fr_0.8fr]">
          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
            <div className="border-b border-slate-100 p-5">
              <h2 className="font-bold text-slate-900">Ranking inteligente de clientes</h2>
              <p className="mt-1 text-xs text-slate-500">Potencial indica valor futuro; interacción pondera frecuencia y cercanía de la última venta.</p>
            </div>
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-100">
                <thead className="bg-slate-50 text-left text-xs uppercase tracking-wide text-slate-500"><tr><th className="px-5 py-3">Cliente</th><th className="px-4 py-3">Ventas / tarifa</th><th className="px-4 py-3">Potencial</th><th className="px-4 py-3">Interacción / riesgo</th><th className="px-4 py-3">Acción sugerida</th></tr></thead>
                <tbody className="divide-y divide-slate-100">
                  {data.clients.map((client, index) => <tr key={client.clientId} className="align-top hover:bg-slate-50/70">
                    <td className="px-5 py-4"><div className="flex gap-3"><span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-slate-900 text-xs font-bold text-white">{index + 1}</span><div><div className="flex flex-wrap items-center gap-2"><p className="font-semibold text-slate-900">{client.name}</p><Segment value={client.segment} /></div><p className="text-xs text-slate-400">{client.email || 'Sin email'} · última venta {client.lastSale}</p><p className="mt-1 max-w-xs truncate text-[11px] text-sky-700" title={client.preferredAirline}>Prefiere: {client.preferredAirline}</p></div></div></td>
                    <td className="px-4 py-4"><p className="font-semibold text-slate-800">{client.sales.toLocaleString('es-CO')}</p><p className="text-xs text-emerald-700">{money(client.revenue)}</p><p className="mt-1 text-[11px] text-slate-400">Ticket prom. {money(client.averageTicket)}</p></td>
                    <td className="px-4 py-4"><Score value={client.potentialScore} /><span className="mt-1 block text-[11px] text-slate-500">{levelLabel[client.level]}</span></td>
                    <td className="px-4 py-4"><Score value={client.interactionScore} /><span className="mt-1 block text-[11px] text-slate-500">Interacción · riesgo {client.churnRiskScore}/100</span><span className="block text-[11px] text-slate-400">{client.daysSinceLastSale} días · {client.airlinesUsed} aerolíneas</span></td>
                    <td className="max-w-xs px-4 py-4 text-xs leading-5 text-slate-600">{client.recommendation}</td>
                  </tr>)}
                </tbody>
              </table>
              {!data.clients.length && <p className="p-10 text-center text-sm text-slate-400">No hay ventas cerradas con cliente en el rango seleccionado. Usa el periodo disponible indicado arriba.</p>}
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="mb-5 flex items-center gap-3"><div className="rounded-xl bg-sky-50 p-2 text-sky-600"><Plane size={22} /></div><div><h2 className="font-bold text-slate-900">Aerolíneas líderes</h2><p className="text-xs text-slate-500">Participación por tarifa vendida</p></div></div>
            <div className="space-y-4">{data.airlines.map((airline, index) => <div key={airline.name}>
              <div className="mb-1.5 flex items-end justify-between gap-3"><div><p className="text-sm font-semibold text-slate-800">{index + 1}. {airline.name}</p><p className="text-xs text-slate-400">{airline.sales} ventas · {airline.clients} clientes</p></div><div className="text-right"><p className="text-sm font-bold text-slate-900">{money(airline.revenue)}</p><p className="text-xs text-sky-700">{airline.revenueShare.toFixed(1)}%</p></div></div>
              <div className="h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-gradient-to-r from-blue-600 to-sky-400" style={{ width: `${Math.min(100, airline.revenueShare)}%` }} /></div>
            </div>)}</div>
            {!data.airlines.length && <p className="py-10 text-center text-sm text-slate-400">Sin aerolíneas en el periodo.</p>}
          </div>
        </section>

        <div className="flex items-start gap-3 rounded-xl border border-blue-100 bg-blue-50 p-4 text-xs leading-5 text-blue-900"><CalendarDays size={18} className="mt-0.5 shrink-0" /><div><strong>Cómo se calcula:</strong> {data.methodology} El ingreso utiliza la columna Tarifa y no incluye impuestos. Este análisis apoya la decisión comercial; no reemplaza el criterio del equipo.</div></div>
      </>}
    </div>
  );
}

function Score({ value }: { value: number }) {
  return <div className="w-28"><div className="mb-1 flex justify-between text-xs"><span className="font-bold text-slate-800">{value}</span><span className="text-slate-400">/100</span></div><div className="h-1.5 overflow-hidden rounded-full bg-slate-100"><div className={`h-full rounded-full ${scoreColor(value)}`} style={{ width: `${value}%` }} /></div></div>;
}

function SummaryCard({ icon: Icon, eyebrow, title, value, color }: { icon: typeof Users; eyebrow: string; title: string; value: string; color: 'emerald' | 'sky' | 'violet' }) {
  const classes = { emerald: 'bg-emerald-50 text-emerald-600', sky: 'bg-sky-50 text-sky-600', violet: 'bg-violet-50 text-violet-600' }[color];
  return <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><div className={`mb-4 inline-flex rounded-xl p-2.5 ${classes}`}><Icon size={22} /></div><p className="text-xs font-semibold uppercase tracking-wide text-slate-400">{eyebrow}</p><p className="mt-1 truncate text-lg font-black text-slate-900" title={title}>{title}</p><p className="mt-1 text-sm font-semibold text-slate-600">{value}</p></div>;
}

function MiniMetric({ label, value, detail, tone = 'blue' }: { label: string; value: string; detail: string; tone?: 'blue' | 'amber' }) {
  return <div className={`rounded-2xl border p-4 ${tone === 'amber' ? 'border-amber-200 bg-amber-50' : 'border-blue-100 bg-blue-50'}`}><p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{label}</p><p className="mt-1 text-2xl font-black text-slate-900">{value}</p><p className="mt-1 text-xs text-slate-500">{detail}</p></div>;
}

function Segment({ value }: { value: TourismAiClient['segment'] }) {
  const style = value === 'VIP' ? 'bg-emerald-100 text-emerald-700' : value === 'RECUPERAR' ? 'bg-red-100 text-red-700' : value === 'FRECUENTE' ? 'bg-blue-100 text-blue-700' : value === 'CRECIMIENTO' ? 'bg-violet-100 text-violet-700' : 'bg-slate-100 text-slate-600';
  return <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${style}`}>{value}</span>;
}
