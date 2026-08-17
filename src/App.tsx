import { useRef, useCallback } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import {Card,CardContent} from "@/shared/ui/card";

import Schema from "@/schema/Schema";
import AppHeader from "@/header/AppHeader";
import { LoginPage } from "@/components/auth/LoginPage";
import { ProtectedRoute } from "@/components/auth/ProtectedRoute";
import { PublicRoute } from "@/components/auth/PublicRoute";
import { ManualSettingsWidget } from "@/components/settings/ManualSettingsWidget";
import { TempSettingsWidget } from "@/components/settings/TempSettingsWidget";
import { RecuperatorWidget } from "@/components/settings/RecuperatorWidget";
import { ConfiguratorWidget } from "@/components/settings/ConfiguratorWidget";
import SystemEventsJournal from "@/components/logs/SystemEventsJournal";
import DashboardLogs from "@/components/logs/DashboardLogs";
import UpdateSettings from "@/components/settings/UpdateSettings";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import { Sheet, SheetContent, SheetHeader, SheetTitle } from "@/shared/ui/sheet";
import { useAuth } from "@/contexts/AuthContext";
import { SettingsSheetProvider, useSettingsSheet } from "@/contexts/SettingsSheetContext";
import { RecuperatorTypeProvider } from "@/hooks/useRecuperatorType";
import { SettingsPanelBoundsProvider } from "@/contexts/SettingsPanelBoundsContext";
import { cn } from "@/shared/lib/utils";

const SWIPE_THRESHOLD = 60;

const settingsTabsGridClass = (tabCount: number) => {
	if (tabCount <= 3) return "grid-cols-2 md:grid-cols-3";
	if (tabCount === 4) return "grid-cols-2 md:grid-cols-4";
	if (tabCount === 5) return "grid-cols-2 md:grid-cols-5";
	return "grid-cols-3 md:grid-cols-7";
};

const SettingsTabs = ({
	isAdmin,
	canSeeConfigurator,
	compact,
}: {
	isAdmin: boolean;
	canSeeConfigurator: boolean;
	compact?: boolean;
}) => {
	const tabCount = 3 + (canSeeConfigurator ? 1 : 0) + (isAdmin ? 3 : 0);

	return (
		<Tabs defaultValue="volume" className="h-full flex flex-col min-h-0">
			<TabsList
				className={cn(
					"grid w-full mb-4",
					settingsTabsGridClass(tabCount),
					compact && "mb-4 flex w-full flex-nowrap justify-start overflow-x-auto"
				)}
			>
				<TabsTrigger value="volume" className={compact ? "flex-none px-4" : ""}>
					Объем
				</TabsTrigger>
				<TabsTrigger value="temperature" className={compact ? "flex-none px-4" : ""}>
					Температура
				</TabsTrigger>
				<TabsTrigger value="recuperator" className={compact ? "flex-none px-4" : ""}>
					Рекуператор
				</TabsTrigger>
				{canSeeConfigurator && (
					<TabsTrigger value="config" className={compact ? "flex-none px-4" : ""}>
						Редактор
					</TabsTrigger>
				)}
				{isAdmin && <TabsTrigger value="errors" className={compact ? "flex-none px-4" : ""}>Ошибки</TabsTrigger>}
				{isAdmin && <TabsTrigger value="logs" className={compact ? "flex-none px-4" : ""}>Логи</TabsTrigger>}
				{isAdmin && <TabsTrigger value="update" className={compact ? "flex-none px-4" : ""}>Обновление</TabsTrigger>}
			</TabsList>
			<TabsContent value="volume" className="flex-1 overflow-y-auto">
				<ManualSettingsWidget />
			</TabsContent>
			<TabsContent value="temperature" className="flex-1 overflow-y-auto">
				<TempSettingsWidget />
			</TabsContent>
			<TabsContent value="recuperator" className="flex-1 overflow-y-auto">
				<RecuperatorWidget />
			</TabsContent>
			{canSeeConfigurator && (
				<TabsContent value="config" className="flex-1 overflow-y-auto">
					<ConfiguratorWidget />
				</TabsContent>
			)}
			{isAdmin && (
				<TabsContent value="errors" className="flex-1 overflow-y-auto">
					<SystemEventsJournal />
				</TabsContent>
			)}
			{isAdmin && (
				<TabsContent value="logs" className="flex-1 overflow-y-auto">
					<DashboardLogs />
				</TabsContent>
			)}
			{isAdmin && (
				<TabsContent value="update" className="flex-1 overflow-y-auto">
					<UpdateSettings />
				</TabsContent>
			)}
		</Tabs>
	);
};

const MainDashboard = () => {
	const { role } = useAuth();
	const sheet = useSettingsSheet();
	const isAdmin = role === "ROLE_ADMIN";
	const canSeeConfigurator = role === "ROLE_ADMIN" || role === "ROLE_CONFIG";
	const touchStartX = useRef(0);

	const handleTouchStart = useCallback((e: React.TouchEvent) => {
		touchStartX.current = e.touches[0].clientX;
	}, []);

	const handleTouchEnd = useCallback((e: React.TouchEvent) => {
		const deltaX = e.changedTouches[0].clientX - touchStartX.current;
		if (deltaX > SWIPE_THRESHOLD && sheet) {
			sheet.setOpen(true);
		}
	}, [sheet]);

	return (
		<div className="w-full h-full pb-3">
			<div className="md:block h-full">
				<div className="grid grid-cols-1 md:grid-cols-2 gap-6 h-full">
					<Card className="overflow-hidden col-span-1 flex flex-col pt-0 pb-0 h-full rounded-[24px] border border-slate-200 bg-white shadow-[0_20px_25px_-5px_rgba(0,0,0,0.1),0_10px_10px_-5px_rgba(0,0,0,0.04)]">
						<CardContent className="flex-1 flex flex-col h-full px-0">
							<div
								className="w-full h-full flex items-center justify-center touch-pan-y"
								onTouchStart={handleTouchStart}
								onTouchEnd={handleTouchEnd}
							>
								<Schema />
							</div>
						</CardContent>
					</Card>
					
					{/* Правая часть - десктоп */}
					<div className='hidden md:block h-full min-h-0 overflow-hidden'>
						<Card className="h-full min-h-0 flex flex-col rounded-[24px] border border-slate-200 bg-white">
							<CardContent className="flex flex-1 flex-col min-h-0 p-4 lg:p-5 xl:p-6">
								<SettingsPanelBoundsProvider>
									<SettingsTabs isAdmin={isAdmin} canSeeConfigurator={canSeeConfigurator} compact={false} />
								</SettingsPanelBoundsProvider>
							</CardContent>
						</Card>
					</div>
				</div>
			</div>

			{/* Мобильная панель уставок — свайп вправо или кнопка */}
			{sheet && (
			<Sheet open={sheet.open} onOpenChange={sheet.setOpen}>
				<SheetContent side="left" className="w-[92vw] max-w-[360px] md:hidden bg-white p-0 flex flex-col overflow-hidden">
					<SheetHeader className="border-b border-slate-100 px-5 pr-12 pt-5 pb-4 shrink-0">
						<SheetTitle className="text-lg">Уставки</SheetTitle>
					</SheetHeader>
					<div className="flex min-h-0 flex-1 flex-col overflow-y-auto px-5 py-4 pb-8">
						<SettingsPanelBoundsProvider>
							<SettingsTabs isAdmin={isAdmin} canSeeConfigurator={canSeeConfigurator} compact />
						</SettingsPanelBoundsProvider>
					</div>
				</SheetContent>
			</Sheet>
			)}
		</div>
	);
};

const App = () => {
	return (
		<>
			<Routes>
				<Route
					path="/login"
					element={
						<PublicRoute>
							<LoginPage />
						</PublicRoute>
					}
				/>
				<Route
					path="/"
					element={
						<ProtectedRoute>
							<RecuperatorTypeProvider>
								<SettingsSheetProvider>
									<div
										className="flex flex-1 flex-col w-full h-screen overflow-hidden"
										style={{ overscrollBehavior: 'none' }}
									>
										<div className="flex items-center px-4 py-2">
											<AppHeader />
										</div>
										<div className="flex flex-1 overflow-hidden">
											<div className="px-4 lg:px-6 w-full h-full">
												<MainDashboard />
											</div>
										</div>
									</div>
								</SettingsSheetProvider>
							</RecuperatorTypeProvider>
						</ProtectedRoute>
					}
				/>
				<Route path="*" element={<Navigate to="/" replace />} />
			</Routes>
		</>
	);
};

export default App;
