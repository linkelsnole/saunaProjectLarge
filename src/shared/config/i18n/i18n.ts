import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'

import en from './locales/en.json'
import ru from './locales/ru.json'

i18n
	.use(LanguageDetector)
	.use(initReactI18next)
	.init({
		resources: {
			en: {
				translation: en,
			},
			ru: {
				translation: ru,
			},
		},
		supportedLngs: ['en', 'ru'],
		load: 'languageOnly', // This ensures ru-RU becomes ru
		fallbackLng: 'ru',
		debug: false,

		interpolation: {
			escapeValue: false,
		},
	})

export default i18n
