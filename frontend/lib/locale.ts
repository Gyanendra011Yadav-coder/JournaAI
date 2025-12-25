export interface LocaleOption {
  code: string;
  label: string;
}

const fallbackCountries: LocaleOption[] = [
  { code: "us", label: "United States" },
  { code: "in", label: "India" },
  { code: "gb", label: "United Kingdom" },
];

const fallbackLanguages: LocaleOption[] = [
  { code: "en", label: "English" },
  { code: "hi", label: "Hindi" },
];

export function getCountryOptions(): LocaleOption[] {
  if (typeof Intl === "undefined" || typeof (Intl as any).supportedValuesOf !== "function") {
    return fallbackCountries;
  }
  try {
    const display = new Intl.DisplayNames(["en"], { type: "region" });
    const regions = (Intl as any).supportedValuesOf("region") as string[];
    return regions
      .map((code) => ({
        code: code.toLowerCase(),
        label: display.of(code) ?? code,
      }))
      .filter((option) => option.label)
      .sort((a, b) => a.label.localeCompare(b.label));
  } catch (err) {
    return fallbackCountries;
  }
}

export function getLanguageOptions(): LocaleOption[] {
  if (typeof Intl === "undefined" || typeof (Intl as any).supportedValuesOf !== "function") {
    return fallbackLanguages;
  }
  try {
    const display = new Intl.DisplayNames(["en"], { type: "language" });
    const languages = (Intl as any).supportedValuesOf("language") as string[];
    return languages
      .map((code) => ({
        code: code.toLowerCase(),
        label: display.of(code) ?? code,
      }))
      .filter((option) => option.label)
      .sort((a, b) => a.label.localeCompare(b.label));
  } catch (err) {
    return fallbackLanguages;
  }
}
