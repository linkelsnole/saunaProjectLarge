import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  updateManualSettings,
  updateTempSettings,
  updateRecuperatorSettings,
  getServerState,
  getUpgradeStoredUrl,
  setUpgradeStoredUrl,
  getUpgradeLoadStatus,
  startUpgradeDownload,
  getUpgradeInstallStatus,
  startUpgradeInstall,
  getLogSettings,
  updateLogSettings,
  getUserActionLogs,
  downloadReadingsFile,
} from './deviceApi';
import type { DashboardData, UpdateRecuperatorSettingsRequest, RecuperatorSettingsDto, UserActionEntry } from './typesApi';
import { toast } from 'sonner';
import i18n from '@/shared/config/i18n/i18n';

export const deviceKeys = {
  dashboard: () => ['dashboard'] as const,
};

interface UpdateManualSettingsRequest {
  supplySpeed?: number;
  boostPercent?: number;
}

interface UpdateTempSettingsRequest {
  targetZone1?: number;
  targetZone2?: number;
  waterWarnThreshold?: number;
  waterFailThreshold?: number;
  inflowWarnThreshold?: number;
  inflowFailThreshold?: number;

  neutralZoneT?: number;
  hotValveMinProc?: number;
  waterMinTNoWork?: number;
  waterMinTWork?: number;

  valveStartupPosition?: number;
  controlAlgorithm?: number;
  tempTrackingInterval?: number;
}

export const useUpdateManualSettings = () => {
  const queryClient = useQueryClient();
  const dashboardKey = deviceKeys.dashboard();

  return useMutation({
    mutationFn: async (payload: UpdateManualSettingsRequest) => {
      return updateManualSettings(payload);
    },

    // Оптимистичное обновление
    onMutate: async (newSettings) => {
      await queryClient.cancelQueries({ queryKey: dashboardKey });
      const previousDashboard = queryClient.getQueryData<DashboardData>(dashboardKey);

      queryClient.setQueryData<DashboardData>(dashboardKey, (old) => {
        if (!old || !old.manualSettings) return old;

        const newData = { ...old };
        const manual = { ...newData.manualSettings };

        if (newSettings.supplySpeed !== undefined) {
          manual.supplySpeed = newSettings.supplySpeed;
        }
        if (newSettings.boostPercent !== undefined) {
          manual.boostPercent = newSettings.boostPercent;
        }

        newData.manualSettings = manual;
        return newData;
      });

      return { previousDashboard };
    },


    onSuccess: () => {
      toast.success(i18n.t('toasts.volumeSaved'));
    },

    onError: (_err, _newSettings, context) => {
      if (context?.previousDashboard) {
        queryClient.setQueryData(dashboardKey, context.previousDashboard);
      }
      toast.error(i18n.t('toasts.saveError'));
    },

    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: dashboardKey });
    },
  });
};

export const useUpdateTempSettings = () => {
  const queryClient = useQueryClient();
  const dashboardKey = deviceKeys.dashboard();

  return useMutation({
    mutationFn: async (payload: UpdateTempSettingsRequest) => {
      return updateTempSettings(payload);
    },


    onMutate: async (newSettings) => {
      await queryClient.cancelQueries({ queryKey: dashboardKey });
      const previousDashboard = queryClient.getQueryData<DashboardData>(dashboardKey);

      queryClient.setQueryData<DashboardData>(dashboardKey, (old) => {
        if (!old || !old.tempSettings) return old;

        const newData = { ...old };
        const temp = { ...newData.tempSettings };

        if (newSettings.targetZone1 !== undefined) temp.targetZone1 = newSettings.targetZone1;
        if (newSettings.targetZone2 !== undefined) temp.targetZone2 = newSettings.targetZone2;
        if (newSettings.waterWarnThreshold !== undefined) temp.waterWarnThreshold = newSettings.waterWarnThreshold;
        if (newSettings.waterFailThreshold !== undefined) temp.waterFailThreshold = newSettings.waterFailThreshold;
        if (newSettings.inflowWarnThreshold !== undefined) temp.inflowWarnThreshold = newSettings.inflowWarnThreshold;
        if (newSettings.inflowFailThreshold !== undefined) temp.inflowFailThreshold = newSettings.inflowFailThreshold;

        if (newSettings.neutralZoneT !== undefined) temp.neutralZoneT = newSettings.neutralZoneT;
        if (newSettings.hotValveMinProc !== undefined) temp.hotValveMinProc = newSettings.hotValveMinProc;
        if (newSettings.waterMinTNoWork !== undefined) temp.waterMinTNoWork = newSettings.waterMinTNoWork;
        if (newSettings.waterMinTWork !== undefined) temp.waterMinTWork = newSettings.waterMinTWork;

        if (newSettings.valveStartupPosition !== undefined) temp.valveStartupPosition = newSettings.valveStartupPosition ?? null;
        if (newSettings.controlAlgorithm !== undefined) temp.controlAlgorithm = newSettings.controlAlgorithm ?? null;
        if (newSettings.tempTrackingInterval !== undefined) temp.tempTrackingInterval = newSettings.tempTrackingInterval ?? null;

        newData.tempSettings = temp;
        return newData;
      });

      return { previousDashboard };
    },

    onSuccess: () => {
      toast.success(i18n.t('toasts.tempSaved'));
    },

    onError: (_err, _newSettings, context) => {
      if (context?.previousDashboard) {
        queryClient.setQueryData(dashboardKey, context.previousDashboard);
      }
      toast.error(i18n.t('toasts.tempSaveError'));
    },

    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: dashboardKey });
    },
  });
};

export const useServerState = (enabled: boolean) => {
  return useQuery({
    queryKey: ['server-state'],
    queryFn: getServerState,
    enabled,
    staleTime: 60_000,
  });
};

export const useUpgradeStoredUrl = (enabled: boolean) => {
  return useQuery({
    queryKey: ['upgrade-stored-url'],
    queryFn: getUpgradeStoredUrl,
    enabled,
  });
};

export const useSaveUpgradeStoredUrl = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: setUpgradeStoredUrl,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['upgrade-stored-url'] });
    },
  });
};

export const useUpgradeLoadStatus = () => {
  return useMutation({
    mutationFn: (url?: string) => getUpgradeLoadStatus(url),
    onError: () => {
      toast.error(i18n.t('toasts.upgradeLoadStatusError'));
    },
  });
};

export const useStartUpgradeDownload = () => {
  return useMutation({
    mutationFn: (url?: string) => startUpgradeDownload(url),
    onError: () => {
      toast.error(i18n.t('toasts.upgradeDownloadError'));
    },
  });
};

export const useUpgradeInstallStatus = () => {
  return useMutation({
    mutationFn: getUpgradeInstallStatus,
    onError: () => {
      toast.error(i18n.t('toasts.upgradeInstallStatusError'));
    },
  });
};

export const useStartUpgradeInstall = () => {
  return useMutation({
    mutationFn: () => startUpgradeInstall(),
    onError: () => {
      toast.error(i18n.t('toasts.upgradeInstallError'));
    },
  });
};

export const useLogSettings = () => {
  return useQuery({
    queryKey: ['logs', 'settings'],
    queryFn: getLogSettings,
    staleTime: 30000,
  });
};

export const useUpdateLogSettings = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (intervalMinutes: number) => updateLogSettings(intervalMinutes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['logs', 'settings'] });
      toast.success(i18n.t('toasts.intervalSaved'));
    },
    onError: () => {
      toast.error(i18n.t('toasts.intervalSaveError'));
    },
  });
};

export const useUserActionLogs = (minutes: number, type: string) => {
  return useQuery({
    queryKey: ['logs', 'actions', minutes, type],
    queryFn: () => getUserActionLogs(minutes, type),
    refetchInterval: 15000,
    placeholderData: (prev: UserActionEntry[] | undefined) => prev,
  });
};

export const useDownloadReadings = () => {
  return useMutation({
    mutationFn: ({ from, to }: { from: number; to: number }) => downloadReadingsFile(from, to),
    onSuccess: () => {
      toast.success(i18n.t('toasts.readingsDownloaded'));
    },
    onError: () => {
      toast.error(i18n.t('toasts.readingsDownloadError'));
    },
  });
};

export const useUpdateRecuperatorSettings = () => {
  const queryClient = useQueryClient();
  const dashboardKey = deviceKeys.dashboard();

  return useMutation({
    mutationFn: async (payload: UpdateRecuperatorSettingsRequest) => {
      return updateRecuperatorSettings(payload);
    },

    onMutate: async (newSettings) => {
      await queryClient.cancelQueries({ queryKey: dashboardKey });
      const previousDashboard = queryClient.getQueryData<DashboardData>(dashboardKey);

      queryClient.setQueryData<DashboardData>(dashboardKey, (old) => {
        if (!old || !old.recuperatorSettings) return old;

        const newData = { ...old };
        const recuperator = {
          targetRpmMin: newSettings.targetRpmMin !== undefined ? (newSettings.targetRpmMin ?? null) : old.recuperatorSettings.targetRpmMin,
          targetRpmMax: newSettings.targetRpmMax !== undefined ? (newSettings.targetRpmMax ?? null) : old.recuperatorSettings.targetRpmMax,
          targetRpmStep: newSettings.targetRpmStep !== undefined ? (newSettings.targetRpmStep ?? null) : old.recuperatorSettings.targetRpmStep,
          trackingIntervalRecup:
            newSettings.trackingIntervalRecup !== undefined
              ? (newSettings.trackingIntervalRecup ?? null)
              : old.recuperatorSettings.trackingIntervalRecup,
        } as RecuperatorSettingsDto;

        newData.recuperatorSettings = recuperator;
        return newData;
      });

      return { previousDashboard };
    },

    onSuccess: () => {
      toast.success(i18n.t('toasts.recupSaved'));
    },

    onError: (_err, _newSettings, context) => {
      if (context?.previousDashboard) {
        queryClient.setQueryData(dashboardKey, context.previousDashboard);
      }
      toast.error(i18n.t('toasts.recupSaveError'));
    },

    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: dashboardKey });
    },
  });
};
