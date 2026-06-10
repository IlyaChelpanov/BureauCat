import { useQuery } from '@tanstack/react-query'

async function fetchHealth(): Promise<{ status: string }> {
  const res = await fetch('/api/health')
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  return res.json()
}

function App() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['health'],
    queryFn: fetchHealth,
  })

  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-50">
      <div className="rounded-lg bg-white p-8 shadow">
        <h1 className="text-2xl font-bold text-gray-900">BureauCat</h1>
        <p className="mt-2 text-gray-600">
          Backend:{' '}
          {isLoading && <span className="text-gray-400">проверка…</span>}
          {isError && <span className="text-red-600">недоступен</span>}
          {data && <span className="text-green-600">{data.status}</span>}
        </p>
      </div>
    </main>
  )
}

export default App
