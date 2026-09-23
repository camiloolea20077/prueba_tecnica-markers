/**
 * Envoltorio estándar de todas las respuestas del backend.
 */
export interface ApiResponse<T> {
  status: number
  message: string
  error: boolean
  data: T
}
