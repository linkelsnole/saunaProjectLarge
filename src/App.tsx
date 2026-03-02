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
import { SystemEventsWidget } from "@/components/logs/SystemEventsWidget";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import { useAuth } from "@/contexts/AuthContext";

const MainDashboard = () => {
	const { role } = useAuth();
	const isAdmin = role === "ROLE_ADMIN";

	return (
		<div className="w-full h-full pb-3">
			<div className="md:block h-full">
				<div className="grid grid-cols-1 md:grid-cols-2 gap-6 h-full">
					<Card className="overflow-hidden col-span-1 flex flex-col pt-0 pb-0 h-full rounded-[24px] border border-slate-200 bg-white shadow-[0_20px_25px_-5px_rgba(0,0,0,0.1),0_10px_10px_-5px_rgba(0,0,0,0.04)]">
						<CardContent className="flex-1 flex flex-col h-full px-0">
							<div className="w-full h-full flex items-center justify-center">
								<Schema />
							</div>
						</CardContent>
					</Card>
					
					{/* Правая часть - все вкладки на одном уровне */}
					<div className='hidden md:block h-full overflow-hidden'>
					<Card className="h-full rounded-[24px] border border-slate-200 bg-white">
							<CardContent className="p-4 lg:p-5 xl:p-6 h-full">
								<Tabs defaultValue="volume" className="h-full flex flex-col">
									<TabsList className={isAdmin ? "grid w-full grid-cols-4 mb-4" : "grid w-full grid-cols-3 mb-4"}>
										<TabsTrigger value="volume">Объем</TabsTrigger>
										<TabsTrigger value="temperature">Температура</TabsTrigger>
										<TabsTrigger value="recuperator">Рекуператор</TabsTrigger>
										{isAdmin && <TabsTrigger value="logs">Логи</TabsTrigger>}
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
									{isAdmin && (
										<TabsContent value="logs" className="flex-1 overflow-hidden">
											<SystemEventsWidget />
										</TabsContent>
									)}
								</Tabs>
							</CardContent>
						</Card>
					</div>
				</div>
			</div>
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
							<div 
								className="flex flex-1 flex-col w-full h-screen overflow-hidden"
								style={{ overscrollBehavior: 'none' }}
							>
								<div className="flex items-center px-4 py-2">
									<AppHeader/>
								</div>
								<div className="flex flex-1 overflow-hidden">
									<div className="px-4 lg:px-6 w-full h-full">
										<MainDashboard />
									</div>
								</div>
							</div>
						</ProtectedRoute>
					}
				/>
				<Route path="*" element={<Navigate to="/" replace />} />
			</Routes>
		</>
	);
};

export default App;
