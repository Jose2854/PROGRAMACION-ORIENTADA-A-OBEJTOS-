export interface TourismAiClient {
  clientId: number;
  name: string;
  email?: string;
  sales: number;
  revenue: number;
  lastSale: string;
  daysSinceLastSale: number;
  airlinesUsed: number;
  preferredAirline: string;
  averageTicket: number;
  potentialScore: number;
  interactionScore: number;
  churnRiskScore: number;
  segment: 'VIP' | 'RECUPERAR' | 'FRECUENTE' | 'CRECIMIENTO' | 'NUTRIR';
  level: 'MUY_ALTO' | 'ALTO' | 'MEDIO' | 'POR_ACTIVAR';
  recommendation: string;
}

export interface TourismAiAirline {
  name: string;
  sales: number;
  revenue: number;
  clients: number;
  revenueShare: number;
}

export interface TourismAiInsights {
  from: string;
  to: string;
  availableFrom?: string;
  availableTo?: string;
  generatedAt: string;
  methodology: string;
  summary: {
    topClient: string;
    topClientRevenue: number;
    topAirline: string;
    topAirlineRevenue: number;
    highestPotentialClient: string;
    highestPotentialScore: number;
    highPotentialClients: number;
    atRiskClients: number;
    topAirlineConcentration: number;
  };
  clients: TourismAiClient[];
  airlines: TourismAiAirline[];
}
