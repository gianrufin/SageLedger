import React from 'react';
import { AlertTriangle, RefreshCw } from 'lucide-react';

interface ErrorBoundaryState {
  hasError: boolean;
  message: string;
}

export class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  ErrorBoundaryState
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false, message: '' };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return {
      hasError: true,
      message: error.message || 'An unexpected error occurred.',
    };
  }

  componentDidCatch(error: Error): void {
    console.error('ErrorBoundary caught error:', error);
  }

  render(): React.ReactNode {
    if (!this.state.hasError) {
      return this.props.children;
    }

    let friendlyDetail = this.state.message;
    try {
      const parsed = JSON.parse(this.state.message) as {
        error?: string;
        operationType?: string;
        path?: string | null;
      };
      if (parsed && typeof parsed.error === 'string') {
        friendlyDetail = `${parsed.error}${parsed.path ? ` (${parsed.operationType ?? 'op'} at ${parsed.path})` : ''}`;
      }
    } catch {
      // Plain error string
    }

    return (
      <div className="min-h-screen flex items-center justify-center p-6 bg-[var(--bg-canvas)] text-[var(--ink-primary)]">
        <div className="w-full max-w-md rounded-3xl bg-[var(--bg-surface)] border border-[var(--hairline)] p-6 space-y-4">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-[var(--coral-container)] text-[var(--coral-tertiary)] flex items-center justify-center shrink-0">
              <AlertTriangle className="w-5 h-5" />
            </div>
            <div>
              <h1 className="text-lg font-bold tracking-tight">Something went wrong</h1>
              <p className="text-xs text-[var(--ink-muted)]">
                SageLedger intercepted an application error to protect your session.
              </p>
            </div>
          </div>
          <p className="text-sm text-[var(--ink-muted)] bg-[var(--bg-surface-low)] border border-[var(--hairline)] rounded-2xl p-3 break-words">
            {friendlyDetail}
          </p>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="w-full inline-flex items-center justify-center gap-2 rounded-full bg-[var(--mint-primary)] text-white dark:text-[#042419] font-semibold py-3 px-4 text-sm hover:opacity-90 transition cursor-pointer"
          >
            <RefreshCw className="w-4 h-4" />
            Reload SageLedger
          </button>
        </div>
      </div>
    );
  }
}
