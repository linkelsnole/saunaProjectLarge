import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import '@/shared/config/i18n/i18n'
import './styles/main.css'
import App from './App'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ToastProvider } from '@/providers/ToastProvider'
import { store } from '@/store/Store'
import { Provider } from 'react-redux'
import { AuthProvider } from '@/contexts/AuthContext'



const queryClient = new QueryClient({
	defaultOptions: {
		queries: {
			refetchOnWindowFocus: false,
			retry: 1,
			staleTime: 30000,
		},
	},
})

const root = ReactDOM.createRoot(document.getElementById('root') as HTMLElement)
root.render(
	<Provider store={store}>
		<QueryClientProvider client={queryClient}>
			<BrowserRouter>
				<AuthProvider>
					<App />
					<ToastProvider />
				</AuthProvider>
			</BrowserRouter>
		</QueryClientProvider>
	</Provider>
);

