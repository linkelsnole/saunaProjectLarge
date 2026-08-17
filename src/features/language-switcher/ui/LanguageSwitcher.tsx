import { useTranslation } from 'react-i18next'
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuTrigger,
} from "@/shared/ui/dropdown-menu"
import { Button } from "@/shared/ui/button"
import { Globe } from "lucide-react"

export const LanguageSwitcher = () => {
	const { i18n } = useTranslation()

	const changeLanguage = (lng: string) => {
		i18n.changeLanguage(lng)
	}

	return (
		<DropdownMenu>
			<DropdownMenuTrigger asChild>
				<Button variant="ghost" size="icon" className="h-9 w-9">
					<Globe className="h-4 w-4" />
					<span className="sr-only">Toggle language</span>
				</Button>
			</DropdownMenuTrigger>
			<DropdownMenuContent align="end">
				<DropdownMenuItem onClick={() => changeLanguage('ru')}>
					Русский
				</DropdownMenuItem>
				<DropdownMenuItem onClick={() => changeLanguage('en')}>
					English
				</DropdownMenuItem>
			</DropdownMenuContent>
		</DropdownMenu>
	)
}
