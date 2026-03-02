export interface ClimateSensorDto {
  id: string;
  temperature: number | null;
  humidity: number | null;
  label: string;
}

export interface SensorData {
  id: string;
  name: string;
  value: string | null;
  unit: string;
}

export interface FanFlowData {
  type: string;
  speedPerc: number | null;
  vm: number | null;
  pressure: number | null;
  targetAirFlow: number | null;
}

export interface RecuperatorStatusDto {
  enabled: boolean;
  stateCode: number | null;
  stateName: string;
}

export interface ManualSettingsDto {
  supplySpeed: number | null;
  boostPercent: number | null;
}

export interface TempSettingsDto {
  targetZone1: number | null;
  targetZone2: number | null;
  waterWarnThreshold: number | null;
  waterFailThreshold: number | null;
  inflowWarnThreshold: number | null;
  inflowFailThreshold: number | null;
  ctrlModeZone1: number | null;
  ctrlModeZone2: number | null;

  // Новые поля для отладки (Ввод)
  neutralZoneT?: number | null;      // DTNeutral
  hotValveMinProc?: number | null;   // HotValveMinProc
  waterMinTNoWork?: number | null;   // TWaterNoWork
  waterMinTWork?: number | null;     // TWaterWork
  
  // Новые поля (Только чтение)
  radTempFail?: number | null;       // TRadFzFail
}

export interface RecuperatorSettingsDto {
  targetRpmMin: number | null;   // Минимальные обороты (об/мин) - сейчас 0 или 0.5 оборотов
  targetRpmMax: number | null;   // Максимальные обороты (об/мин) - сейчас от 1 до 7 оборотов может стоять
  targetRpmStep: number | null;  // Шаг калибровки (об/мин) - сейчас 0.1 или 0.3 оборота
}

export interface DashboardData {
  climateSensors: ClimateSensorDto[];
  wSensors: SensorData[];
  pressureSensors: SensorData[];
  fans: FanFlowData[];
  valves: SensorData[];
  recuperator: SensorData[];
  recuperatorStatus: RecuperatorStatusDto;
  vm1: number | null;
  vm2: number | null;
  recupSpeed: number | null;
  vManualProcIn: number | null;
  vManualDProcOut: number | null;
  manualSettings: ManualSettingsDto;
  tempSettings: TempSettingsDto;
  recuperatorSettings?: RecuperatorSettingsDto;
  damperSupply: boolean | null;   // задвижка притока
  damperExhaust: boolean | null;   // задвижка вытяжки
  pumpActive?: boolean | null;     // Workstate2 бит 14 — насос рекуператора (команда)
  recupStateCode?: number | null;  // RecupState 0x00A4
  recupStateName?: string | null;  // Отключен, Макс. обороты, Настройка, Стабилизация, Врем. остановка

  debugValveSensors?: SensorData[];
  
  // Новые регистры рекуператора
  recupTargetRpm?: number | null;   // Обороты вала рекуператора (целевые), об/мин
  recupCurrentRpm?: number | null;  // Обороты вала, об/мин
  recupSpeedRpm?: number | null;    // Скорость вращения, об/мин
  recupTempBt6?: number | null;    // Температура рекуператора BT6, °C
  recupTargetPercent?: number | null; // recupTargetRpm / targetRpmMax * 100, %
}

export interface UpdateRecuperatorSettingsRequest {
  targetRpmMin?: number;
  targetRpmMax?: number;
  targetRpmStep?: number;
}

export interface LegacyDateTime {
  timeInMS?: number;
}

export interface SystemEvent {
  type: number;
  level: number;
  title: string;
  comment: string;
  arrivalTime?: LegacyDateTime;
  endTime?: LegacyDateTime;
  equipName?: string;
  devName?: string;
  regNum?: number;
  bitNum?: number;
  prevValue?: number;
  value?: number;
  secondRegNum?: number;
  secondRegValue?: number;
  viewLevel?: number;
  packedValues?: string;
  logUnit?: number;
  facadeClass?: string;
}
