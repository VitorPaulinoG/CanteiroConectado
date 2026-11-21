type Opcoes = {
  value: Date;
  minimumDate?: Date;
  onChange?: (evento: { type: 'set' | 'dismissed' }, data?: Date) => void;
};

let proximaData: Date | null = null;

export function __escolherData(data: Date | null) {
  proximaData = data;
}

export const DateTimePickerAndroid = {
  open: jest.fn((opcoes: Opcoes) => {
    if (proximaData) {
      opcoes.onChange?.({ type: 'set' }, proximaData);
    } else {
      opcoes.onChange?.({ type: 'dismissed' });
    }
  }),
  dismiss: jest.fn(),
};

export default function DateTimePicker() {
  return null;
}
