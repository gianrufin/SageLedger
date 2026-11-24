import React, { useEffect, useMemo, useRef, useState } from 'react';
import { onAuthStateChanged, signOut, type User } from 'firebase/auth';
import { Home, PieChart, Plus, Settings, Target, Wallet } from 'lucide-react';
import { auth } from './lib/firebase';
import {
  addRecurringPreset,
  addTransaction,
  defaultSettingsFor,
  deleteMerchantRule,
  deleteRecurringPreset,
  deleteTransaction,
  ensureDefaultSettings,
  observeBudgets,
  observeMerchantRules,
  observeRecurringPresets,
  observeSettings,
  observeTransactions,
  saveSettings,
  updateTransaction,
  upsertCategoryBudget,
  upsertMerchantRule,
} from './lib/ledgerRepository';
import {
  computePeriodReport,
  computeSavingsProjection,
  currentMonthBounds,
} from './lib/analytics';
import { checkForLatestGitHubRelease } from './lib/githubUpdates';
import {
  applyThemeClass,
  loadDismissedReleaseTag,
  loadOnboardingComplete,
  loadThemeMode,
  saveDismissedReleaseTag,
  saveOnboardingComplete,
  saveThemeMode,
} from './lib/preferences';
import {
  ReportPeriod,
  ThemeModePreference,
  type CategoryBudget,
  type DashboardSnapshot,
  type GitHubReleaseNotice,
  type MerchantRule,
  type RecurringPreset,
  type SpendingCategory,
  type SpendingTransaction,
  type UserFinanceSettings,
} from './types/ledger';
import { AuthScreen, OnboardingScreen } from './components/AuthAndOnboarding';
import { AccountProfileModal, AlertsInboxModal } from './components/HeaderSheets';
import { AddRecurringPresetModal, ManualEntryModal } from './components/ManualEntryModal';
import { OverviewView } from './components/OverviewView';
import { PlanView } from './components/PlanView';
import { ReportsView } from './components/ReportsView';
import { SettingsView } from './components/SettingsView';

type AppTab = 'overview' | 'reports' | 'plan' | 'settings';

export function App() {
  const [authReady, setAuthReady] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [onboardingComplete, setOnboardingComplete] = useState<boolean>(() =>
    loadOnboardingComplete()
  );
  const [themeMode, setThemeMode] = useState<ThemeModePreference>(() => loadThemeMode());
  const [activeTab, setActiveTab] = useState<AppTab>('overview');

  // Firestore synced state
  const [transactions, setTransactions] = useState<SpendingTransaction[]>([]);
  const [budgets, setBudgets] = useState<CategoryBudget[]>([]);
  const [recurringPresets, setRecurringPresets] = useState<RecurringPreset[]>([]);
  const [merchantRules, setMerchantRules] = useState<MerchantRule[]>([]);
  const [settings, setSettings] = useState<UserFinanceSettings | null>(null);
  const [dataLoading, setDataLoading] = useState(true);
  const [dataError, setDataError] = useState<string | null>(null);
  const [asyncError, setAsyncError] = useState<Error | null>(null);

  // Reports filters
  const [selectedPeriod, setSelectedPeriod] = useState<ReportPeriod>(ReportPeriod.MONTH);
  const [customStartMillis, setCustomStartMillis] = useState<number | null>(null);
  const [customEndMillis, setCustomEndMillis] = useState<number | null>(null);

  // Modals & notifications
  const [showAccountModal, setShowAccountModal] = useState(false);
  const [showAlertsModal, setShowAlertsModal] = useState(false);
  const [showEntryModal, setShowEntryModal] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState<SpendingTransaction | null>(null);
  const [showAddPresetModal, setShowAddPresetModal] = useState(false);
  const [updateNotice, setUpdateNotice] = useState<GitHubReleaseNotice | null>(null);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const lastCheckedRepoRef = useRef<string | null>(null);
  const toastTimerRef = useRef<number | null>(null);

  if (asyncError) {
    throw asyncError;
  }

  const showToast = (msg: string) => {
    setToastMessage(msg);
    if (toastTimerRef.current) window.clearTimeout(toastTimerRef.current);
    toastTimerRef.current = window.setTimeout(() => setToastMessage(null), 3200);
  };

  // Theme application + system listener
  useEffect(() => {
    applyThemeClass(themeMode);
    if (themeMode !== ThemeModePreference.SYSTEM || !window.matchMedia) return;
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = () => applyThemeClass(ThemeModePreference.SYSTEM);
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, [themeMode]);

  const handleSetThemeMode = (mode: ThemeModePreference) => {
    setThemeMode(mode);
    saveThemeMode(mode);
  };

  // Auth listener
  useEffect(() => {
    const unsub = onAuthStateChanged(auth, (firebaseUser) => {
      setUser(firebaseUser);
      setAuthReady(true);
      if (!firebaseUser) {
        setTransactions([]);
        setBudgets([]);
        setRecurringPresets([]);
        setMerchantRules([]);
        setSettings(null);
        setDataLoading(false);
        setDataError(null);
        lastCheckedRepoRef.current = null;
      }
    });
    return () => unsub();
  }, []);

  // Firestore real-time listeners once authenticated
  useEffect(() => {
    if (!authReady || !user) return;
    const uid = user.uid;
    setDataLoading(true);
    setDataError(null);

    ensureDefaultSettings(uid).catch((err) => {
      setAsyncError(err instanceof Error ? err : new Error(String(err)));
    });

    const unsubTx = observeTransactions(uid, (items) => {
      setTransactions(items);
      setDataLoading(false);
    });
    const unsubBudgets = observeBudgets(uid, setBudgets);
    const unsubPresets = observeRecurringPresets(uid, setRecurringPresets);
    const unsubRules = observeMerchantRules(uid, setMerchantRules);
    const unsubSettings = observeSettings(uid, (s) => {
      setSettings(s);
      setDataLoading(false);
    });

    return () => {
      unsubTx();
      unsubBudgets();
      unsubPresets();
      unsubRules();
      unsubSettings();
    };
  }, [authReady, user]);

  // Automatic GitHub update check when repo is configured
  useEffect(() => {
    const repo = settings?.githubRepo.trim();
    if (!user || !repo || lastCheckedRepoRef.current === repo) return;
    lastCheckedRepoRef.current = repo;
    checkForLatestGitHubRelease(repo)
      .then((notice) => {
        if (notice && notice.tagName !== loadDismissedReleaseTag()) {
          setUpdateNotice(notice);
        }
      })
      .catch(() => {
        // Silent for background check
      });
  }, [user, settings?.githubRepo]);

  const activeSettings = useMemo(() => {
    if (settings) return settings;
    return defaultSettingsFor(user?.uid ?? '');
  }, [settings, user?.uid]);

  const snapshot: DashboardSnapshot | null = useMemo(() => {
    if (!user) return null;
    const [mStart, mEnd] = currentMonthBounds();
    const monthTx = transactions.filter(
      (tx) => tx.occurredAtMillis >= mStart && tx.occurredAtMillis <= mEnd
    );
    const monthSpentCents = monthTx.reduce((sum, tx) => sum + tx.amountCents, 0);
    const projection = computeSavingsProjection(
      transactions,
      activeSettings.monthlyIncomeCents,
      activeSettings.monthlyBudgetCents
    );
    const unreviewedCount = transactions.filter((tx) => tx.needsReview).length;

    return {
      settings: activeSettings,
      transactions,
      monthTransactions: monthTx,
      monthSpentCents,
      budgets,
      recurringPresets,
      merchantRules,
      savingsProjection: projection,
      unreviewedCount,
    };
  }, [user, transactions, activeSettings, budgets, recurringPresets, merchantRules]);

  const periodReport = useMemo(
    () =>
      computePeriodReport(
        transactions,
        budgets,
        selectedPeriod,
        customStartMillis,
        customEndMillis
      ),
    [transactions, budgets, selectedPeriod, customStartMillis, customEndMillis]
  );

  const pendingAlerts = useMemo(
    () => transactions.filter((tx) => tx.needsReview),
    [transactions]
  );

  if (!authReady) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[var(--bg-canvas)]">
        <div className="w-8 h-8 border-2 border-[var(--mint-primary)] border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  if (!onboardingComplete && !user) {
    return (
      <OnboardingScreen
        onFinish={() => {
          saveOnboardingComplete(true);
          setOnboardingComplete(true);
        }}
      />
    );
  }

  if (!user) {
    return <AuthScreen />;
  }

  const userName = user.displayName ?? '';
  const userEmail = user.email ?? '';
  const userInitial = (userName || userEmail || 'S').trim().charAt(0).toUpperCase();

  const handleQuickAddPreset = async (preset: RecurringPreset) => {
    await addTransaction(user.uid, {
      merchant: preset.merchant,
      amountCents: preset.amountCents,
      category: preset.category,
      paymentSource: preset.paymentSource,
      note: 'Quick-add shortcut',
      occurredAtMillis: Date.now(),
      needsReview: false,
    });
    showToast(`Logged ${preset.label}`);
  };

  const handleConfirmTransaction = async (tx: SpendingTransaction) => {
    if (!tx.needsReview) return;
    await updateTransaction(user.uid, {
      transactionId: tx.id,
      merchant: tx.merchant,
      amountCents: tx.amountCents,
      category: tx.category,
      paymentSource: tx.paymentSource,
      note: tx.note,
      occurredAtMillis: tx.occurredAtMillis,
      needsReview: false,
    });
    showToast(`Confirmed ${tx.merchant}`);
  };

  const handleCheckUpdateManually = async (repo: string) => {
    const clean = repo.trim();
    if (!clean) {
      showToast('Enter a GitHub repository (owner/repo) first');
      return;
    }
    try {
      const notice = await checkForLatestGitHubRelease(clean);
      if (notice) {
        setUpdateNotice(notice);
      } else {
        showToast('Up to date — no newer release found');
      }
    } catch {
      showToast('Failed to check GitHub releases');
    }
  };

  const navItems: Array<{ key: AppTab; label: string; icon: React.ReactNode }> = [
    { key: 'overview', label: 'Overview', icon: <Home className="w-[18px] h-[18px]" /> },
    { key: 'reports', label: 'Reports', icon: <PieChart className="w-[18px] h-[18px]" /> },
    { key: 'plan', label: 'Plan', icon: <Target className="w-[18px] h-[18px]" /> },
    { key: 'settings', label: 'Settings', icon: <Settings className="w-[18px] h-[18px]" /> },
  ];

  return (
    <div className="min-h-screen bg-[var(--bg-canvas)] text-[var(--ink-primary)] flex">
      {/* Desktop Sidebar Rail */}
      <aside className="hidden md:flex md:flex-col md:w-64 md:fixed md:inset-y-0 border-r border-[var(--hairline)] bg-[var(--bg-surface)] px-4 py-6 justify-between z-30">
        <div className="space-y-6">
          <div className="flex items-center gap-3 px-2">
            <div className="w-10 h-10 rounded-xl bg-[var(--mint-container)] border border-[var(--mint-primary)]/40 text-[var(--mint-primary)] flex items-center justify-center">
              <Wallet className="w-5 h-5" />
            </div>
            <div>
              <div className="font-extrabold text-base leading-none tracking-tight">SageLedger</div>
              <div className="font-mono-data text-[9.5px] font-bold tracking-[0.1em] uppercase text-[var(--ink-muted)] mt-1">
                SPENDING COCKPIT
              </div>
            </div>
          </div>

          <button
            type="button"
            onClick={() => {
              setEditingTransaction(null);
              setShowEntryModal(true);
            }}
            data-testid="desktop_add_spend_button"
            className="w-full h-11 rounded-full bg-[var(--mint-primary)] text-white dark:text-[#042419] font-bold text-sm inline-flex items-center justify-center gap-2 hover:opacity-95 transition cursor-pointer"
          >
            <Plus className="w-4 h-4" />
            <span>Add Spending</span>
          </button>

          <nav className="space-y-1">
            {navItems.map((item) => {
              const selected = activeTab === item.key;
              return (
                <button
                  key={item.key}
                  type="button"
                  onClick={() => setActiveTab(item.key)}
                  data-testid={`nav_${item.key}`}
                  className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-2xl text-sm font-bold transition cursor-pointer ${
                    selected
                      ? 'bg-[var(--mint-container)] text-[var(--on-mint-container)]'
                      : 'text-[var(--ink-muted)] hover:bg-[var(--bg-surface-low)] hover:text-[var(--ink-primary)]'
                  }`}
                >
                  {item.icon}
                  <span>{item.label}</span>
                </button>
              );
            })}
          </nav>
        </div>

        <button
          type="button"
          onClick={() => setShowAccountModal(true)}
          className="flex items-center gap-2.5 p-2.5 rounded-2xl hover:bg-[var(--bg-surface-low)] transition text-left cursor-pointer"
        >
          <div className="w-9 h-9 rounded-full bg-[var(--mint-container)] text-[var(--mint-primary)] flex items-center justify-center text-sm font-extrabold shrink-0">
            {userInitial}
          </div>
          <div className="min-w-0">
            <div className="text-xs font-bold text-[var(--ink-primary)] truncate">
              {userName || 'Account'}
            </div>
            <div className="text-[11px] text-[var(--ink-muted)] truncate">{userEmail}</div>
          </div>
        </button>
      </aside>

      {/* Main Content */}
      <main className="flex-1 md:pl-64 min-w-0">
        {activeTab === 'overview' && (
          <OverviewView
            snapshot={snapshot}
            loading={dataLoading}
            error={dataError}
            updateNotice={updateNotice}
            userInitial={userInitial}
            userName={userName}
            themeMode={themeMode}
            onQuickAddPreset={handleQuickAddPreset}
            onAddPresetClicked={() => setShowAddPresetModal(true)}
            onTransactionClick={(tx) => {
              setEditingTransaction(tx);
              setShowEntryModal(true);
            }}
            onConfirmTransaction={handleConfirmTransaction}
            onManualEntryClick={() => {
              setEditingTransaction(null);
              setShowEntryModal(true);
            }}
            onOpenAccount={() => setShowAccountModal(true)}
            onOpenAlerts={() => setShowAlertsModal(true)}
            onDismissUpdate={(permanent) => {
              if (permanent && updateNotice) {
                saveDismissedReleaseTag(updateNotice.tagName);
              }
              setUpdateNotice(null);
            }}
            onNavigateToReports={() => setActiveTab('reports')}
            onNavigateToPlan={() => setActiveTab('plan')}
          />
        )}

        {activeTab === 'reports' && (
          <ReportsView
            report={periodReport}
            selectedPeriod={selectedPeriod}
            currencyCode={activeSettings.currencyCode}
            monthlyBudgetCents={activeSettings.monthlyBudgetCents}
            onSelectPeriod={setSelectedPeriod}
            onSelectCustomRange={(s, e) => {
              setCustomStartMillis(s);
              setCustomEndMillis(e);
              setSelectedPeriod(ReportPeriod.CUSTOM);
            }}
          />
        )}

        {activeTab === 'plan' && snapshot && (
          <PlanView
            snapshot={snapshot}
            onSaveCategoryBudget={async (cat: SpendingCategory, cents: number) => {
              await upsertCategoryBudget(user.uid, cat, cents);
              showToast('Category budget updated');
            }}
            onQuickAddPreset={handleQuickAddPreset}
            onDeletePreset={async (id) => {
              await deleteRecurringPreset(user.uid, id);
              showToast('Shortcut deleted');
            }}
            onAddPresetClicked={() => setShowAddPresetModal(true)}
            onOpenSettings={() => setActiveTab('settings')}
          />
        )}

        {activeTab === 'settings' && (
          <SettingsView
            snapshot={snapshot}
            userEmail={userEmail}
            userName={userName}
            userInitial={userInitial}
            themeMode={themeMode}
            onSetThemeMode={handleSetThemeMode}
            onSaveFinanceSettings={async (payload) => {
              await saveSettings(user.uid, payload);
              showToast('Budget preferences saved');
            }}
            onSaveCategoryBudget={async (cat, cents) => {
              await upsertCategoryBudget(user.uid, cat, cents);
              showToast('Category budget updated');
            }}
            onDeletePreset={async (id) => {
              await deleteRecurringPreset(user.uid, id);
              showToast('Shortcut deleted');
            }}
            onAddPresetClicked={() => setShowAddPresetModal(true)}
            onSaveMerchantRule={async (keyword, cat) => {
              await upsertMerchantRule(user.uid, keyword, cat);
              showToast('Store rule saved');
            }}
            onDeleteMerchantRule={async (id) => {
              await deleteMerchantRule(user.uid, id);
              showToast('Store rule removed');
            }}
            onCheckGitHubUpdate={handleCheckUpdateManually}
            onSignOut={() => signOut(auth)}
          />
        )}
      </main>

      {/* Mobile Floating Glass Bottom Dock + + FAB */}
      <div className="md:hidden fixed bottom-4 inset-x-4 z-30 flex items-center gap-2.5 max-w-lg mx-auto">
        <div
          className="flex-1 flex items-center justify-around rounded-full border border-[var(--hairline)] p-1.5 backdrop-blur-md shadow-lg"
          style={{ backgroundColor: 'var(--glass-dock)' }}
        >
          {navItems.map((item) => {
            const selected = activeTab === item.key;
            return (
              <button
                key={item.key}
                type="button"
                onClick={() => setActiveTab(item.key)}
                className={`flex flex-col items-center justify-center px-3 py-1.5 rounded-full text-[10px] font-bold transition cursor-pointer ${
                  selected
                    ? 'bg-[var(--mint-primary)] text-white dark:text-[#042419]'
                    : 'text-[var(--ink-muted)]'
                }`}
              >
                {item.icon}
                <span className="mt-0.5">{item.label}</span>
              </button>
            );
          })}
        </div>

        <button
          type="button"
          onClick={() => {
            setEditingTransaction(null);
            setShowEntryModal(true);
          }}
          data-testid="add_spend_fab"
          aria-label="Add cash or manual spend"
          className="w-14 h-14 rounded-full bg-[var(--mint-primary)] text-white dark:text-[#042419] shadow-lg flex items-center justify-center shrink-0 active:scale-95 transition cursor-pointer"
        >
          <Plus className="w-6 h-6" />
        </button>
      </div>

      {/* Modals */}
      {showAccountModal && (
        <AccountProfileModal
          userName={userName}
          userEmail={userEmail}
          userInitial={userInitial}
          currencyCode={activeSettings.currencyCode}
          monthlyBudgetCents={activeSettings.monthlyBudgetCents}
          monthlyIncomeCents={activeSettings.monthlyIncomeCents}
          themeMode={themeMode}
          onSetThemeMode={handleSetThemeMode}
          onOpenSettings={() => setActiveTab('settings')}
          onSignOut={() => signOut(auth)}
          onClose={() => setShowAccountModal(false)}
        />
      )}

      {showAlertsModal && (
        <AlertsInboxModal
          pendingAlerts={pendingAlerts}
          currencyCode={activeSettings.currencyCode}
          merchantRules={merchantRules}
          onConfirmAlert={handleConfirmTransaction}
          onEditAlert={(tx) => {
            setEditingTransaction(tx);
            setShowEntryModal(true);
          }}
          onCaptureAlertToInbox={async (payload) => {
            await addTransaction(user.uid, {
              ...payload,
              note: 'Parsed alert',
              occurredAtMillis: Date.now(),
              needsReview: true,
            });
            showToast(`Added ${payload.merchant} to Alerts Inbox`);
          }}
          onClose={() => setShowAlertsModal(false)}
        />
      )}

      {showEntryModal && (
        <ManualEntryModal
          editingTransaction={editingTransaction}
          currencyCode={activeSettings.currencyCode}
          merchantRules={merchantRules}
          onSave={async (payload) => {
            if (payload.existingId) {
              await updateTransaction(user.uid, {
                transactionId: payload.existingId,
                merchant: payload.merchant,
                amountCents: payload.amountCents,
                category: payload.category,
                paymentSource: payload.paymentSource,
                note: payload.note,
                occurredAtMillis: payload.occurredAtMillis,
                needsReview: false,
              });
              showToast('Purchase confirmed & updated');
            } else {
              await addTransaction(user.uid, {
                merchant: payload.merchant,
                amountCents: payload.amountCents,
                category: payload.category,
                paymentSource: payload.paymentSource,
                note: payload.note,
                rawSnippet: payload.rawSnippet,
                occurredAtMillis: payload.occurredAtMillis,
                needsReview: false,
              });
              showToast('Spending recorded');
            }
            if (payload.rememberMerchantRule) {
              await upsertMerchantRule(user.uid, payload.merchant, payload.category);
            }
          }}
          onDelete={async (id) => {
            await deleteTransaction(user.uid, id);
            showToast('Transaction deleted');
          }}
          onClose={() => {
            setShowEntryModal(false);
            setEditingTransaction(null);
          }}
        />
      )}

      {showAddPresetModal && (
        <AddRecurringPresetModal
          currencyCode={activeSettings.currencyCode}
          onSave={async (payload) => {
            await addRecurringPreset(user.uid, payload);
            showToast(`Added quick-add ${payload.label}`);
          }}
          onClose={() => setShowAddPresetModal(false)}
        />
      )}

      {/* Toast Feedback */}
      {toastMessage && (
        <div className="fixed bottom-22 md:bottom-6 right-4 md:right-6 z-50 rounded-full bg-[var(--contrast-pill-bg)] text-[var(--contrast-pill-text)] text-xs font-bold px-4 py-2.5 shadow-lg border border-[var(--hairline)]">
          {toastMessage}
        </div>
      )}
    </div>
  );
}

export default App;
