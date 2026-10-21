import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ManualSettingsWidget } from './ManualSettingsWidget';
import { TempSettingsWidget } from './TempSettingsWidget';
import { RecuperatorWidget } from './RecuperatorWidget';
import { ScheduleWorkspace } from './ScheduleWorkspace';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs';

export const SettingsWidget = () => {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState('volume');

  return (
    <div className="h-full flex flex-col overflow-hidden">
      <Tabs value={activeTab} onValueChange={setActiveTab} className="flex-1 min-h-0 h-full flex flex-col overflow-hidden">
        <TabsList
          className="grid w-full mb-4 shrink-0 grid-cols-4"
        >
          <TabsTrigger value="volume" className="text-xs sm:text-sm">
            {t('app.tabsShort.volume')}
          </TabsTrigger>
          <TabsTrigger value="temperature" className="text-xs sm:text-sm">
            {t('app.tabsShort.temperature')}
          </TabsTrigger>
          <TabsTrigger value="recuperator" className="text-xs sm:text-sm">
            {t('app.tabsShort.recuperator')}
          </TabsTrigger>
          <TabsTrigger value="schedule" className="text-xs sm:text-sm">
            {t('app.tabs.schedule')}
          </TabsTrigger>

        </TabsList>

        <TabsContent value="volume" className="flex-1 overflow-y-auto animate-in fade-in duration-500 motion-reduce:animate-none">
          <ManualSettingsWidget />
        </TabsContent>

        <TabsContent value="temperature" className="flex-1 overflow-y-auto animate-in fade-in duration-500 motion-reduce:animate-none">
          <TempSettingsWidget />
        </TabsContent>

        <TabsContent value="recuperator" className="flex-1 overflow-y-auto animate-in fade-in duration-500 motion-reduce:animate-none">
          <RecuperatorWidget />
        </TabsContent>

        <TabsContent value="schedule" className="flex-1 overflow-y-auto animate-in fade-in duration-500 motion-reduce:animate-none">
          <ScheduleWorkspace />
        </TabsContent>


      </Tabs>
    </div>
  );
};
