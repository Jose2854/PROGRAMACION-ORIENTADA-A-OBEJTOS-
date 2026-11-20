export interface TourismMetric {
  ventas: number;
  pasajeros: number;
  ventasConCosto: number;
  ingreso: number;
  costo: number;
  margen: number;
}

export interface TourismReport {
  from: string;
  to: string;
  totals: TourismMetric;
  ventasPorDia: Record<string, TourismMetric>;
  ventasPorCliente: Record<string, TourismMetric>;
  ventasPorTipoProducto: Record<string, TourismMetric>;
  ventasPorAerolinea: Record<string, TourismMetric>;
  ventasPorHotel: Record<string, TourismMetric>;
  ventasPorProveedor: Record<string, TourismMetric>;
  ventasPorDestino: Record<string, TourismMetric>;
  ventasPorCanal: Record<string, TourismMetric>;
  ventasPorMoneda: Record<string, TourismMetric>;
}
