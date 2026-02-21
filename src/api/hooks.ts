import { useMutation, useQueryClient } from '@tanstack/react-query';
import { updateManualSettings, updateTempSettings, updateRecuperatorSettings } from './deviceApi';
import type { DashboardData, UpdateRecuperatorSettingsRequest, RecuperatorSettingsDto } from './typesApi';
import { toast } from 'sonner';

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
      toast.success('Настройки объема сохранены');
    },

    onError: (_err, _newSettings, context) => {
      if (context?.previousDashboard) {
        queryClient.setQueryData(dashboardKey, context.previousDashboard);
      }
      toast.error('Не удалось сохранить настройки');
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

        newData.tempSettings = temp;
        return newData;
      });

      return { previousDashboard };
    },

    onSuccess: () => {
      toast.success('Температурные уставки сохранены');
    },

    onError: (_err, _newSettings, context) => {
      if (context?.previousDashboard) {
        queryClient.setQueryData(dashboardKey, context.previousDashboard);
      }
      toast.error('Не удалось сохранить температурные уставки');
    },

    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: dashboardKey });
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
        } as RecuperatorSettingsDto;

        newData.recuperatorSettings = recuperator;
        return newData;
      });

      return { previousDashboard };
    },

    onSuccess: () => {
      toast.success('Уставки рекуператора сохранены');
    },

    onError: (_err, _newSettings, context) => {
      if (context?.previousDashboard) {
        queryClient.setQueryData(dashboardKey, context.previousDashboard);
      }
      toast.error('Не удалось сохранить уставки рекуператора');
    },

    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: dashboardKey });
    },
  });
};
