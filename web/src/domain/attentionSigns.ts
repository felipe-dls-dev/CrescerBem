// Sinais de atenção exibidos depois de registrar um sintoma.
//
// Isto NÃO é triagem nem diagnóstico: só destaca combinações em que vale procurar
// um profissional, sempre acompanhadas de Texts.ATTENTION_ADVICE.

import { dateOf, daysBetween } from './dates';
import type { HealthEvent, LocalDate } from './types';

const YOUNG_INFANT_DAYS = 90;

export const AttentionSigns = {
  /** Temperatura axilar a partir da qual se considera febre. */
  FEVER_CELSIUS: 37.8,
  HIGH_TEMPERATURE_CELSIUS: 38.0,
  LOW_TEMPERATURE_CELSIUS: 35.5,

  evaluate(event: HealthEvent, birthDate: LocalDate): string[] {
    const signs: string[] = [];
    const ageDays = daysBetween(birthDate, dateOf(event.dateTime));
    const youngInfant = ageDays >= 0 && ageDays < YOUNG_INFANT_DAYS;
    const temperature = event.temperatureCelsius ?? null;
    const symptoms = new Set(event.symptoms);
    const hasFever = symptoms.has('FEVER') || (temperature !== null && temperature >= AttentionSigns.FEVER_CELSIUS);

    if (hasFever && youngInfant) signs.push('Febre em bebê com menos de 3 meses.');
    if (temperature !== null && temperature >= AttentionSigns.HIGH_TEMPERATURE_CELSIUS) {
      signs.push('Temperatura de 38 °C ou mais.');
    }
    if (temperature !== null && temperature < AttentionSigns.LOW_TEMPERATURE_CELSIUS) {
      signs.push('Temperatura abaixo de 35,5 °C.');
    }
    if (symptoms.has('BREATHING_DIFFICULTY')) signs.push('Dificuldade para respirar.');

    const vomiting = symptoms.has('VOMITING');
    const diarrhea = symptoms.has('DIARRHEA');
    if (vomiting && diarrhea) {
      signs.push('Vômito e diarreia juntos podem levar à desidratação.');
    } else if ((vomiting || diarrhea) && event.intensity === 'STRONG') {
      signs.push('Vômito ou diarreia intensos podem levar à desidratação.');
    }
    if (symptoms.has('FEEDING_REFUSAL') && youngInfant) {
      signs.push('Recusa para mamar em bebê com menos de 3 meses.');
    }
    if (event.intensity === 'STRONG' && signs.length === 0) signs.push('Mal-estar com intensidade forte.');
    return signs;
  },
};
