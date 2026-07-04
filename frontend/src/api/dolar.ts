export interface CotizacionDolar {
  compra: number
  venta: number
  fechaActualizacion: string
}

export async function fetchDolarBlue(): Promise<CotizacionDolar> {
  const res = await fetch('https://dolarapi.com/v1/dolares/blue')
  if (!res.ok) throw new Error('No se pudo obtener la cotización del dólar blue')
  return res.json()
}

export async function fetchDolarMep(): Promise<CotizacionDolar> {
  const res = await fetch('https://dolarapi.com/v1/dolares/bolsa')
  if (!res.ok) throw new Error('No se pudo obtener la cotización del dólar MEP')
  return res.json()
}
