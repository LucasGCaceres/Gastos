export type EstadoCiclo = 'ABIERTO' | 'CERRADO'
export type EstadoCuota = 'PENDIENTE' | 'PAGADO'

export interface CicloListItem {
  id: number
  anio: number
  mes: number
  estado: EstadoCiclo
  fechaCierreReal: string | null
  totalIngresos: number
  saldoFinal: number
}

export interface GastoFijoItem {
  id: number
  nombre: string
  monto: number
}

export interface GastoVariable {
  id: number
  categoria: string
  concepto: string
  monto: number
  fecha: string
}

export interface CuotaImputada {
  id: number
  compraId: number
  tarjetaId: number
  concepto: string
  tarjeta: string
  numeroCuota: number
  totalCuotas: number
  montoEnPesos: number
  mesImpacto: number
  anioImpacto: number
  estado: EstadoCuota
}

export interface GastoEsperado {
  calculadoraId: number
  nombre: string
  objetivoUsd: number
}

export interface EventoGastoItem {
  id: number
  concepto: string
  monto: number
}

export interface EventoGasto {
  id: number
  nombre: string
  fecha: string
  total: number
  items: EventoGastoItem[]
}

export interface CicloResumen {
  id: number
  anio: number
  mes: number
  estado: EstadoCiclo
  fechaCierreReal: string | null
  totalIngresos: number
  totalFijos: number
  totalVariables: number
  totalCuotas: number
  saldoFinal: number
  gastosFijos: GastoFijoItem[]
  gastosVariables: GastoVariable[]
  cuotas: CuotaImputada[]
  gastosEsperados: GastoEsperado[]
  eventos: EventoGasto[]
}

export interface Categoria {
  id: number
  nombre: string
  icono: string | null
}

export interface Tarjeta {
  id: number
  nombre: string
  banco: string | null
  diaCierreEstimado: number
  diaVencimientoEstimado: number
  activa: boolean
}
