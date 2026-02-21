import { useQuery } from "@tanstack/react-query";
import { fetchWithAuth } from "./authApi";
import type { SystemEvent } from "./typesApi";

export const useSystemEvents = (minutes: number) => {
  const query = useQuery<SystemEvent[]>({
    queryKey: ["systemEvents", minutes],
    queryFn: async () => {
      const response = await fetchWithAuth(`/api/v1/devices/events?minutes=${minutes}`);
      if (!response.ok) throw new Error("Network response was not ok");
      return response.json();
    },
    refetchInterval: 15000,
  });

  return {
    data: query.data,
    isLoading: query.isLoading,
    error: query.error,
    refetch: query.refetch,
    isFetching: query.isFetching,
  };
};
