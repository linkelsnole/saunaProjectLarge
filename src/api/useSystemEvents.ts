import { useQuery } from '@tanstack/react-query';
import { readSystemEvents } from './systemEvents';
export const useSystemEvents=(minutes:number)=>useQuery({
 queryKey:['systemEvents',minutes],
 queryFn:({signal})=>readSystemEvents(minutes,signal),
 refetchInterval:15000,
 retry:1,
});
