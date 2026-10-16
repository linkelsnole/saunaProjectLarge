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

export interface VolumeSettingsDto {
  ecoAirflow: number | null;
  normalAirflow: number | null;
  intensiveAirflow: number | null;
  manualAirflow: number | null;
  boostPercent: number | null;
}

export interface UpdateVolumeSettingsRequest {
  ecoAirflow?: number;
  normalAirflow?: number;
  intensiveAirflow?: number;
  manualAirflow?: number;
  boostPercent?: number;
}

export interface TempSettingsDto {
  outflowTarget: number | null;
  inflowTargetZone1: number | null;
  inflowTargetZone2: number | null;
  orpZone1: OrpSettingsDto | null;
  orpZone2: OrpSettingsDto | null;
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

  /** Прямое чтение Modbus, без powerproc (приоритет 0) */
  valveStartupPosition?: number | null;   // 0x00E1 положение вентилей при запуске
  controlAlgorithm?: number | null;       // 0x00DD алгоритм управления
  tempTrackingInterval?: number | null;  // 0x007E интервал слежения по T
}

export interface OrpSettingsDto {
  targetT1: number | null;
  currentT1: number | null;
  targetT2: number | null;
  currentT2: number | null;
  targetT3: number | null;
  currentT3: number | null;
}

export interface UpdateTemperatureControlRequest {
  zone1Mode?: 0 | 1 | 2;
  zone2Mode?: 0 | 1 | 2;
}

export interface UpdateTemperatureSetpointsRequest {
  outflowTarget?: number;
  inflowTargetZone1?: number;
  inflowTargetZone2?: number;
  orpZone1?: Partial<OrpSettingsDto>;
  orpZone2?: Partial<OrpSettingsDto>;
}

export interface RecuperatorSettingsDto {
  targetRpmMin: number | null;   // Минимальные обороты (об/мин) - сейчас 0 или 0.5 оборотов
  targetRpmMax: number | null;   // Максимальные обороты (об/мин) - сейчас от 1 до 7 оборотов может стоять
  targetRpmStep: number | null;  // Шаг калибровки (об/мин) - сейчас 0.1 или 0.3 оборота
  trackingIntervalRecup?: number | null; // 0x0511 интервал опроса счётчика оборотов, секунды
}

export interface DeviceStateDto {
  id: string;
  baseAddress: number;
  alarmMode: number | null;
  alarmSeconds: number | null;
}

export interface DashboardData {
  deviceStates?: DeviceStateDto[];
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
  calibration?: { calibrating?: boolean };
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
  ventilationModeCode?: number | null;
  ventilationModeName?: string | null;
  airflowTarget?: number | null;
  airflowReal?: number | null;
  volumeSettings?: VolumeSettingsDto | null;
  temperatureControlModeZone1Code?: number | null;
  temperatureControlModeZone1Name?: string | null;
  temperatureControlModeZone2Code?: number | null;
  temperatureControlModeZone2Name?: string | null;
  screenMode?: boolean | null;
}

export type ScheduleMode = 'NONE' | 'DEFAULT' | 'NIGHT' | 'DAYS_OF_WEEK' | 'DATES' | 'USERSET';
export type ScheduleStatus = 'INACTIVE' | 'ACTIVE';
export type ScheduleVentilationMode = 'DISABLED' | 'ECO' | 'NORMAL' | 'INTENSIVE' | 'MANUAL';
export type ScheduleTemperatureMode = 'DISABLED' | 'ECO' | 'AUTO' | 'MANUAL';
export type TemperatureControlType = 'ORP' | 'PV' | 'OV';

export interface ScheduleData {
  scheduleId: number;
  mode: ScheduleMode;
  status: ScheduleStatus;
  statusRawCode: number;
  weekDayMask: number;
  group: number | null;
  dateRange: { startDate: number; endDate: number };
  timeRange: { startTime: number; endTime: number };
  duration: number;
  airControl: { mode: ScheduleVentilationMode; targetAirflow: number | null };
  temperatureModeZone1: ScheduleTemperatureMode;
  temperatureZone1: { controlType: TemperatureControlType; targetTemperature: number | null };
  temperatureModeZone2: ScheduleTemperatureMode;
  temperatureZone2: { controlType: TemperatureControlType; targetTemperature: number | null };
}

export interface ScheduleListData {
  schedules: ScheduleData[];
  status: string;
  message: string;
  totalSlots: number;
}

export interface UpdateScheduleIntervalRequest {
  scheduleId: number;
  startTime: number;
  endTime: number;
  airControlMode: number;
  zone1ControlMode: number;
  zone1TargetTemperature?: number;
  zone2ControlMode: number;
  zone2TargetTemperature?: number;
}

export interface UpdateScheduleGroupRequest {
  startDate: number;
  endDate: number;
  weekDays: string[];
  intervals: UpdateScheduleIntervalRequest[];
  isActive: boolean;
}

export interface UpdateRecuperatorSettingsRequest {
  targetRpmMin?: number;
  targetRpmMax?: number;
  targetRpmStep?: number;
  /** 0x0511 интервал опроса счётчика оборотов, секунды */
  trackingIntervalRecup?: number;
}

export interface LegacyDateTime {
  timeInMS?: number;
}

export interface DeviceError {
  devName?: string | null;
  devEvtMode?: number | null;
  devEvtRegName?: string | null;
  devEvtRegValue?: string | null;
  level?: number | null;
  title?: string | null;
  mes?: string | null;
  comment?: string | null;
  timeStamp?: number | null;
}

export interface SystemEvent {
  deviceError?: DeviceError | null;
  oid?: string | null;
  valid?: boolean | null;
  type?: number | null;
  level?: number | null;
  title?: string | null;
  comment?: string | null;
  arrivalTime?: {timeInMS?: number | null} | null;
  endTime?: {timeInMS?: number | null} | null;
  equipName?: string | null;
  devName?: string | null;
  regNum?: number | null;
  bitNum?: number | null;
  prevValue?: number | null;
  value?: number | null;
  secondRegNum?: number | null;
  secondRegValue?: number | null;
  viewLevel?: number | null;
  packedValues?: string | null;
  logUnit?: number | null;
  facadeClass?: string | null;
}

export interface UpgradeStatusData {
  errors: string[];
  info: string[];
}

export interface ServerStateData {
  serverRun: boolean;
  locked: boolean;
  requestNum: number;
  sessionCount: number;
  timeSum: number;
  timeMin: number;
  timeMax: number;
  timeCount: number;
  pid: number;
  releaseNumber: number;
  version: string;
  totalGetCount: number;
  inflowTarget: number | null;
  outflowTarget: number | null;
  status: string;
  message: string;
}

export type UserActionType = 'POWER' | 'MODE' | 'SETPOINT' | 'AUTH' | 'OTHER';

export interface UserActionEntry {
  timestamp: number;
  username: string;
  actionType: UserActionType;
  action: string;
  success: boolean;
  details: string | null;
}

export interface LogSettingsData {
  intervalMinutes: number;
}
