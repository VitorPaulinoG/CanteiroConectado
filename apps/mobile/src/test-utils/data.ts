import * as Calendario from '@react-native-community/datetimepicker';

type CalendarioFalso = { __escolherData: (data: Date | null) => void };

export function escolherNoCalendario(data: Date | null) {
  (Calendario as unknown as CalendarioFalso).__escolherData(data);
}
