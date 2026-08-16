import { useQuery } from "@tanstack/react-query";
import type { DashboardData } from "./typesApi";
import { fetchWithAuth } from "./authApi";
import { useAuth } from "@/contexts/AuthContext";

export const useDashboardData = () => {
	const { role } = useAuth();
	const isConfig = role === "ROLE_CONFIG";

	const query = useQuery<DashboardData>({
		queryKey: ["dashboard"],
		queryFn: async () => {
			const response = await fetchWithAuth("/api/v1/devices/dashboard");
			if (!response.ok) throw new Error("Network response was not ok");
			return response.json();
		},
		refetchInterval: isConfig ? false : 7000,
	});

	return {
		data: query.data,
		isLoading: isConfig ? false : query.isLoading,
		error: query.error,
		refetch: query.refetch,
	};
};
