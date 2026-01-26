import { useState } from 'react';
import { ManualSettingsWidget } from './ManualSettingsWidget';
import { TempSettingsWidget } from './TempSettingsWidget';
// import { RecuperatorWidget } from './RecuperatorWidget';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/ui/tabs';

export const SettingsWidget = () => {
  const [activeTab, setActiveTab] = useState('volume');

  return (
    <div className="h-full flex flex-col">
      <Tabs value={activeTab} onValueChange={setActiveTab} className="flex-1 flex flex-col">
        <TabsList className="grid w-full grid-cols-3 mb-4">
          <TabsTrigger value="volume">Объем</TabsTrigger>
          <TabsTrigger value="temperature">Температура</TabsTrigger>
          <TabsTrigger value="recuperator">Рекуператор</TabsTrigger>
        </TabsList>
        
        <TabsContent value="volume" className="flex-1 overflow-y-auto">
          <ManualSettingsWidget />
        </TabsContent>
        
        <TabsContent value="temperature" className="flex-1 overflow-y-auto">
          <TempSettingsWidget />
        </TabsContent>
        
        {/* <TabsContent value="recuperator" className="flex-1 overflow-y-auto">
          <RecuperatorWidget />
        </TabsContent> */}
      </Tabs>
    </div>
  );
};

