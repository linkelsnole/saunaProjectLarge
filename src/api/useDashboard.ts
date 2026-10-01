import { mergeSettingsDashboard } from './settingsQueue';
import { useQuery, useQueryClient } from "@tanstack/react-query";
import type { DashboardData } from "./typesApi";
import { fetchWithAuth } from "./authApi";
import { useAuth } from "@/contexts/AuthContext";

export const useDashboardData = () => {
	const { role } = useAuth();
	const isConfig = role === "ROLE_CONFIG";

	const client = useQueryClient();
	const query = useQuery<DashboardData>({
		queryKey: ["dashboard"],
		queryFn: async ({ signal }) => {
			const response = await fetchWithAuth("/api/v1/devices/dashboard", { signal });
			if (!response.ok) throw new Error("Network response was not ok");
			return mergeSettingsDashboard(client, await response.json());
		},
		refetchInterval: isConfig ? false : 7000,
		refetchOnWindowFocus: true,
	});

	return {
		data: query.data,
		isLoading: isConfig ? false : query.isLoading,
		error: query.error,
		refetch: query.refetch,
	};
};
