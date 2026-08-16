import { useState } from 'react';
import { ManualSettingsWidget } from './ManualSettingsWidget';
import { TempSettingsWidget } from './TempSettingsWidget';
import { RecuperatorWidget } from './RecuperatorWidget';
import { ConfiguratorWidget } from './ConfiguratorWidget';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs';
import { useAuth } from '@/contexts/AuthContext';

export const SettingsWidget = () => {
  const [activeTab, setActiveTab] = useState('volume');
  const { role } = useAuth();
  const canSeeConfigurator = role === 'ROLE_ADMIN' || role === 'ROLE_CONFIG';

  return (
    <div className="h-full flex flex-col overflow-hidden">
      <Tabs value={activeTab} onValueChange={setActiveTab} className="flex-1 min-h-0 h-full flex flex-col overflow-hidden">
        <TabsList
          className={`grid w-full mb-4 shrink-0 ${canSeeConfigurator ? 'grid-cols-4' : 'grid-cols-3'}`}
        >
          <TabsTrigger value="volume" className="text-xs sm:text-sm">
            Объем
          </TabsTrigger>
          <TabsTrigger value="temperature" className="text-xs sm:text-sm">
            Темп.
          </TabsTrigger>
          <TabsTrigger value="recuperator" className="text-xs sm:text-sm">
            Рекуп.
          </TabsTrigger>
          {canSeeConfigurator && (
            <TabsTrigger value="config" className="text-xs sm:text-sm">
              Редактор
            </TabsTrigger>
          )}
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
      </Tabs>
    </div>
  );
};

