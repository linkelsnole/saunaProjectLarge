import React from "react"
import { useNavigate } from "react-router-dom"
import { Avatar, AvatarFallback } from "@/shared/ui/avatar"
import { Button } from "@/shared/ui/button"
import {
	ChevronDownIcon,
	User,
	Settings,
	LogOut,
	RefreshCcw,
	SlidersHorizontal,
} from "lucide-react"
import { useSettingsSheet } from "@/contexts/SettingsSheetContext"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from "@/shared/ui/dropdown-menu"
import { useAuth } from "@/contexts/AuthContext"
import { triggerReconnect } from "@/api/connectionApi"

interface AppHeaderProps {
}

const AppHeader: React.FC<AppHeaderProps> = () => {
	const { username, role, logout } = useAuth()
	const settingsSheet = useSettingsSheet()
	const navigate = useNavigate()
	const isAdmin = role === 'ROLE_ADMIN'

	const handleLogout = () => {
		logout()
		navigate('/login')
	}

	const getInitials = (name: string | null) => {
		if (!name) return 'U'
		return name.substring(0, 2).toUpperCase()
	}

	const getRoleDisplayName = (role: string | null) => {
		if (!role) return 'Пользователь'
		if (role === 'ROLE_ADMIN') return 'Администратор'
		if (role === 'ROLE_READONLY') return 'Только чтение'
		return 'Пользователь'
	}

	return (
		<header className="flex h-8 min-[360px]:h-12 items-center gap-4 bg-transparent w-full">
			<div className="flex items-center gap-4 lg:gap-6 w-full">
				{/* Кнопка уставок — только на мобилке */}
				{settingsSheet && (
					<Button
						variant="ghost"
						size="icon"
						className="md:hidden shrink-0"
						onClick={() => settingsSheet.openSettings()}
						aria-label="Уставки"
					>
						<SlidersHorizontal className="h-5 w-5" />
					</Button>
				)}
				<div className="flex items-center gap-3 ml-auto">
					<DropdownMenu>
						<DropdownMenuTrigger asChild>
							<Button variant="ghost" className="avatar-wrapper relative h-9 px-1 flex items-center gap-2">
								<Avatar className="h-8 w-8">
									<AvatarFallback>{getInitials(username)}</AvatarFallback>
								</Avatar>
								<div className="hidden lg:flex flex-col items-start">
									<span className="text-sm font-medium text-header-title">{username || 'Пользователь'}</span>
									<span className="text-xs text-muted-foreground">{getRoleDisplayName(role)}</span>
								</div>
								<ChevronDownIcon className="h-4 w-4 text-muted-foreground hidden lg:block" />
							</Button>
						</DropdownMenuTrigger>
						<DropdownMenuContent align="end" className="w-56" sideOffset={5}>
							<DropdownMenuLabel className="text-base">My Account</DropdownMenuLabel>
							<DropdownMenuSeparator />
							<DropdownMenuItem className="cursor-pointer text-base">
								<User className="h-4 w-4 mr-2" />
								<span>Profile</span>
							</DropdownMenuItem>
							{isAdmin && (
								<DropdownMenuItem 
									className="cursor-pointer text-base"
									onClick={(e) => {
										e.preventDefault();
										triggerReconnect();
									}}
								>
									<RefreshCcw className="h-4 w-4 mr-2" />
									<span>Reconnect</span>
								</DropdownMenuItem>
							)}
							<DropdownMenuItem className="cursor-pointer text-base">
								<Settings className="h-4 w-4 mr-2" />
								<span>Settings</span>
							</DropdownMenuItem>
							<DropdownMenuSeparator />
							<DropdownMenuItem 
								className="cursor-pointer text-destructive focus:text-destructive text-base"
								onClick={handleLogout}
							>
								<LogOut className="h-4 w-4 mr-2" />
								<span>Logout</span>
							</DropdownMenuItem>
						</DropdownMenuContent>
					</DropdownMenu>
				</div>
			</div>
		</header>
	)
}

export default AppHeader