export interface AdminInterestRateTier {
  id: number
  name: string
  minTermMonths: number
  maxTermMonths: number
  annualEffectiveRate: number
  active: boolean
}

export interface InterestRateTierRequest {
  name: string
  minTermMonths: number
  maxTermMonths: number
  annualEffectiveRate: number
  active: boolean
}
