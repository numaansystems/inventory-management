import type { Category, Unit } from "./api";

const UNIT_SYMBOL: Record<Unit, string> = {
  GRAM: "g",
  KILOGRAM: "kg",
  MILLILITRE: "ml",
  LITRE: "l",
  PIECE: "pc",
};

const UNIT_NAME: Record<Unit, string> = {
  GRAM: "Gram (g)",
  KILOGRAM: "Kilogram (kg)",
  MILLILITRE: "Millilitre (ml)",
  LITRE: "Litre (l)",
  PIECE: "Piece",
};

const number = new Intl.NumberFormat("en-US", { maximumFractionDigits: 3 });
const dateTime = new Intl.DateTimeFormat("en-US", { dateStyle: "medium", timeStyle: "short" });

export function formatNumber(value: number): string {
  return number.format(value);
}

/** "100 g", "1.5 kg" */
export function formatAmount(value: number, unit: Unit): string {
  return `${number.format(value)} ${UNIT_SYMBOL[unit]}`;
}

export function unitName(unit: Unit): string {
  return UNIT_NAME[unit];
}

export function formatDateTime(iso: string): string {
  return dateTime.format(new Date(iso));
}

export function categoryLabel(categories: Category[], code: string | null): string {
  if (code === null) {
    return "Uncategorised";
  }
  return categories.find((category) => category.code === code)?.label ?? code;
}

/** Signed with an explicit plus, e.g. "+20" / "-3". */
export function formatChange(value: number): string {
  return value > 0 ? `+${number.format(value)}` : number.format(value);
}
