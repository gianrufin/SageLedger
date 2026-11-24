import { useEffect, useState } from 'react';
import { Palette, Trash2, X } from 'lucide-react';
import {
  ALL_CATEGORIES,
  CATEGORY_META,
  LIMITS,
  SpendingCategory,
  SUPPORTED_CURRENCIES,
  ThemeModePreference,
  type DashboardSnapshot,
  type SupportedCurrency,
} from '../types/ledger';
import { formatCents, symbolForCurrency } from '../lib/currency';
import { APP_VERSION_NAME } from '../lib/githubUpdates';
import { CrispCard } from './IconsAndVisuals';

export function SettingsView({
  snapshot,
  userEmail,
  userName,
  userInitial,
  themeMode,
  onSetThemeMode,
  onSaveFinanceSettings,
  onSaveCategoryBudget,
  onDeletePreset,
  onAddPresetClicked,
  onSaveMerchantRule,
  onDeleteMerchantRule,
  onCheckGitHubUpdate,
  onSignOut,
}: {
  snapshot: DashboardSnapshot | null;
  userEmail: string;
  userName: string;
  userInitial: string;
  themeMode: ThemeModePreference;
  onSetThemeMode: (mode: ThemeModePreference) => void;
  onSaveFinanceSettings: (payload: {
    monthlyIncomeCents: number;
    monthlyBudgetCents: number;
    currencyCode: string;
    githubRepo: string;
  }) => Promise<void>;
  onSaveCategoryBudget: (category: SpendingCategory, limitCents: number) => Promise<void>;
  onDeletePreset: (id: string) => void;
  onAddPresetClicked: () => void;
  onSaveMerchantRule: (keyword: string, category: SpendingCategory) => Promise<void>;
  onDeleteMerchantRule: (id: string) => void;
  onCheckGitHubUpdate: (repo: string) => Promise<void>;
  onSignOut: () => void;
}) {
  const settings = snapshot?.settings;
  const [incomeInput, setIncomeInput] = useState(
    settings ? Math.round(settings.monthlyIncomeCents / 100).toString() : '4000'
  );
  const [budgetInput, setBudgetInput] = useState(
    settings ? Math.round(settings.monthlyBudgetCents / 100).toString() : '2400'
  );
  const [selectedCurrency, setSelectedCurrency] = useState<SupportedCurrency>(
    settings?.currencyCode ?? 'PHP'
  );
  const [githubRepoInput, setGithubRepoInput] = useState(settings?.githubRepo ?? '');

  const [editingCat, setEditingCat] = useState<SpendingCategory | null>(null);
  const [catLimitText, setCatLimitText] = useState('');

  const [showAddRuleModal, setShowAddRuleModal] = useState(false);
  const [ruleKeyword, setRuleKeyword] = useState('');
  const [ruleCategory, setRuleCategory] = useState<SpendingCategory>(SpendingCategory.FOOD);

  useEffect(() => {
    if (!settings) return;
    setIncomeInput(Math.round(settings.monthlyIncomeCents / 100).toString());
    setBudgetInput(Math.round(settings.monthlyBudgetCents / 100).toString());
    setSelectedCurrency(settings.currencyCode);
    setGithubRepoInput(settings.githubRepo);
  }, [settings]);

  const parseToCents = (val: string) =>
    Math.max(0, Math.min(LIMITS.SETTINGS_MAX_CENTS, Math.round((Number.parseFloat(val) || 0) * 100)));

  return (
    <div className="w-full max-w-[760px] mx-auto px-4 pt-4 pb-28 md:pb-12 space-y-5">
      {/* Edit Category Limit Modal */}
      {editingCat && snapshot && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 backdrop-blur-xs p-4"
          onClick={() => setEditingCat(null)}
        >
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              const cents = Math.max(
                0,
                Math.min(
                  LIMITS.AMOUNT_MAX_CENTS,
                  Math.round((Number.parseFloat(catLimitText) || 0) * 100)
                )
              );
              await onSaveCategoryBudget(editingCat, cents);
              setEditingCat(null);
            }}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-sm rounded-[28px] bg-[var(--bg-surface)] border border-[var(--hairline)] p-6 space-y-4"
          >
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-bold text-[var(--ink-primary)]">
                {CATEGORY_META[editingCat].displayName} Budget
              </h2>
              <button
                type="button"
                onClick={() => setEditingCat(null)}
                className="w-8 h-8 rounded-full hover:bg-[var(--bg-surface-low)] flex items-center justify-center text-[var(--ink-muted)] cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <div>
              <label className="block text-xs font-semibold text-[var(--ink-muted)] mb-1">
                Monthly limit ({selectedCurrency})
              </label>
              <input
                type="text"
                inputMode="decimal"
                value={catLimitText}
                onChange={(e) => {
                  const cleaned = e.target.value.replace(/[^0-9.]/g, '');
                  if ((cleaned.match(/\./g) ?? []).length <= 1) setCatLimitText(cleaned);
                }}
                className="w-full rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] px-3.5 py-2.5 text-base font-bold tabular-nums text-[var(--ink-primary)]"
              />
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setEditingCat(null)}
                className="px-4 py-2 rounded-full text-sm font-semibold text-[var(--ink-muted)] cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-5 py-2 rounded-full bg-[var(--mint-primary)] text-white dark:text-[#042419] text-sm font-bold cursor-pointer"
              >
                Save
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Add Store Rule Modal */}
      {showAddRuleModal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 backdrop-blur-xs p-4"
          onClick={() => setShowAddRuleModal(false)}
        >
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              if (!ruleKeyword.trim()) return;
              await onSaveMerchantRule(ruleKeyword.trim(), ruleCategory);
              setRuleKeyword('');
              setShowAddRuleModal(false);
            }}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-md rounded-[28px] bg-[var(--bg-surface)] border border-[var(--hairline)] p-6 space-y-4"
          >
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-bold text-[var(--ink-primary)]">Add Store Rule</h2>
              <button
                type="button"
                onClick={() => setShowAddRuleModal(false)}
                className="w-8 h-8 rounded-full hover:bg-[var(--bg-surface-low)] flex items-center justify-center text-[var(--ink-muted)] cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
            <input
              type="text"
              value={ruleKeyword}
              maxLength={LIMITS.RULE_KEYWORD_MAX_LEN}
              onChange={(e) => setRuleKeyword(e.target.value)}
              placeholder="Store keyword (e.g. Uncle John, DataBlitz)"
              className="w-full rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] px-3.5 py-2.5 text-sm text-[var(--ink-primary)]"
            />
            <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
              {ALL_CATEGORIES.map((cat) => (
                <button
                  key={cat}
                  type="button"
                  onClick={() => setRuleCategory(cat)}
                  className={`shrink-0 px-3 py-1.5 rounded-full text-xs font-semibold border cursor-pointer ${
                    ruleCategory === cat
                      ? 'bg-[var(--mint-primary)] border-transparent text-white dark:text-[#042419]'
                      : 'bg-[var(--bg-surface-low)] border-[var(--hairline)] text-[var(--ink-muted)]'
                  }`}
                >
                  {CATEGORY_META[cat].shortLabel}
                </button>
              ))}
            </div>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setShowAddRuleModal(false)}
                className="px-4 py-2 rounded-full text-sm font-semibold text-[var(--ink-muted)] cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={!ruleKeyword.trim()}
                className="px-5 py-2 rounded-full bg-[var(--mint-primary)] text-white dark:text-[#042419] text-sm font-bold disabled:opacity-50 cursor-pointer"
              >
                Save
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Header */}
      <div className="flex items-center justify-between gap-3 px-1">
        <div className="min-w-0">
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-[var(--ink-primary)]">
            Settings
          </h1>
          <p className="text-xs text-[var(--ink-muted)] truncate">
            {userEmail || userName || 'Preferences & sync'}
          </p>
        </div>
        <div className="w-10 h-10 rounded-full bg-[var(--mint-container)] text-[var(--on-mint-container)] flex items-center justify-center font-bold shrink-0">
          {userInitial}
        </div>
      </div>

      {/* Appearance */}
      <div className="space-y-2">
        <div className="font-mono-data text-[10.5px] font-bold tracking-[0.08em] uppercase text-[var(--ink-muted)] px-1">
          APPEARANCE
        </div>
        <CrispCard className="p-4 sm:p-5 space-y-3.5">
          <div className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-[var(--mint-container)] text-[var(--mint-primary)] flex items-center justify-center">
                <Palette className="w-[18px] h-[18px]" />
              </div>
              <div>
                <div className="text-sm font-semibold text-[var(--ink-primary)]">Dark mode</div>
                <div className="text-xs text-[var(--ink-muted)]">
                  Crisp obsidian low-glare canvas
                </div>
              </div>
            </div>
            <input
              type="checkbox"
              checked={themeMode === ThemeModePreference.DARK}
              onChange={(e) =>
                onSetThemeMode(
                  e.target.checked ? ThemeModePreference.DARK : ThemeModePreference.LIGHT
                )
              }
              data-testid="dark_mode_switch"
              className="w-5 h-5 accent-[var(--mint-primary)] cursor-pointer"
            />
          </div>

          <div className="flex items-center gap-2">
            {[
              { mode: ThemeModePreference.SYSTEM, label: 'System' },
              { mode: ThemeModePreference.LIGHT, label: 'Light' },
              { mode: ThemeModePreference.DARK, label: 'Dark' },
            ].map(({ mode, label }) => (
              <button
                key={mode}
                type="button"
                onClick={() => onSetThemeMode(mode)}
                className={`px-3.5 py-1.5 rounded-full text-xs font-bold border transition cursor-pointer ${
                  themeMode === mode
                    ? 'bg-[var(--mint-primary)] border-transparent text-white dark:text-[#042419]'
                    : 'bg-[var(--bg-surface-low)] border-[var(--hairline)] text-[var(--ink-muted)]'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
        </CrispCard>
      </div>

      {/* Monthly Income & Budget */}
      <div className="space-y-2">
        <div className="font-mono-data text-[10.5px] font-bold tracking-[0.08em] uppercase text-[var(--ink-muted)] px-1">
          MONTHLY INCOME &amp; BUDGET
        </div>
        <CrispCard className="p-4 sm:p-5 space-y-3.5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-semibold text-[var(--ink-muted)] mb-1">
                Monthly income ({selectedCurrency})
              </label>
              <input
                type="text"
                inputMode="decimal"
                value={incomeInput}
                onChange={(e) => {
                  const cleaned = e.target.value.replace(/[^0-9.]/g, '');
                  if ((cleaned.match(/\./g) ?? []).length <= 1) setIncomeInput(cleaned);
                }}
                className="w-full rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] px-3.5 py-2.5 text-sm font-bold tabular-nums text-[var(--ink-primary)]"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-[var(--ink-muted)] mb-1">
                Monthly spending budget ({selectedCurrency})
              </label>
              <input
                type="text"
                inputMode="decimal"
                value={budgetInput}
                onChange={(e) => {
                  const cleaned = e.target.value.replace(/[^0-9.]/g, '');
                  if ((cleaned.match(/\./g) ?? []).length <= 1) setBudgetInput(cleaned);
                }}
                className="w-full rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] px-3.5 py-2.5 text-sm font-bold tabular-nums text-[var(--ink-primary)]"
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <div className="text-xs font-semibold text-[var(--ink-muted)]">Currency</div>
            <div className="flex items-center gap-2 overflow-x-auto pb-1">
              {SUPPORTED_CURRENCIES.map((code) => (
                <button
                  key={code}
                  type="button"
                  onClick={() => setSelectedCurrency(code)}
                  className={`shrink-0 px-3 py-1.5 rounded-full text-xs font-bold border transition cursor-pointer ${
                    selectedCurrency === code
                      ? 'bg-[var(--mint-primary)] border-transparent text-white dark:text-[#042419]'
                      : 'bg-[var(--bg-surface-low)] border-[var(--hairline)] text-[var(--ink-muted)]'
                  }`}
                >
                  {code} ({symbolForCurrency(code)})
                </button>
              ))}
            </div>
          </div>

          <button
            type="button"
            onClick={() =>
              onSaveFinanceSettings({
                monthlyIncomeCents: parseToCents(incomeInput),
                monthlyBudgetCents: parseToCents(budgetInput),
                currencyCode: selectedCurrency,
                githubRepo: githubRepoInput,
              })
            }
            data-testid="save_finance_settings_button"
            className="w-full h-12 rounded-2xl bg-[var(--mint-primary)] text-white dark:text-[#042419] font-bold text-sm hover:opacity-95 transition cursor-pointer"
          >
            Save budget preferences
          </button>
        </CrispCard>
      </div>

      {snapshot && (
        <>
          {/* Category Limits */}
          <div className="space-y-2">
            <div className="font-mono-data text-[10.5px] font-bold tracking-[0.08em] uppercase text-[var(--ink-muted)] px-1">
              CATEGORY MONTHLY LIMITS
            </div>
            <CrispCard className="p-4 sm:p-5 space-y-2">
              {ALL_CATEGORIES.map((cat, idx) => {
                const limit =
                  snapshot.budgets.find((b) => b.category === cat)?.monthlyLimitCents ??
                  CATEGORY_META[cat].defaultBudgetCents;
                return (
                  <div key={cat}>
                    <div className="flex items-center justify-between py-1.5">
                      <span className="text-sm text-[var(--ink-primary)]">
                        {CATEGORY_META[cat].displayName}
                      </span>
                      <button
                        type="button"
                        onClick={() => {
                          setCatLimitText(Math.round(limit / 100).toString());
                          setEditingCat(cat);
                        }}
                        className="text-xs font-bold tabular-nums text-[var(--mint-primary)] hover:underline cursor-pointer"
                      >
                        {formatCents(limit, selectedCurrency)}
                      </button>
                    </div>
                    {idx < ALL_CATEGORIES.length - 1 && (
                      <div className="h-[1px] bg-[var(--hairline)]/60" />
                    )}
                  </div>
                );
              })}
            </CrispCard>
          </div>

          {/* Recurring Presets */}
          <div className="space-y-2">
            <div className="font-mono-data text-[10.5px] font-bold tracking-[0.08em] uppercase text-[var(--ink-muted)] px-1">
              QUICK-ADD RECURRING BUTTONS
            </div>
            <CrispCard className="p-4 sm:p-5 space-y-3">
              {snapshot.recurringPresets.length === 0 ? (
                <p className="text-sm text-[var(--ink-muted)]">No quick-add buttons saved yet.</p>
              ) : (
                <div className="space-y-2">
                  {snapshot.recurringPresets.map((preset) => (
                    <div key={preset.id} className="flex items-center justify-between gap-2">
                      <div className="min-w-0">
                        <div className="text-sm font-medium text-[var(--ink-primary)] truncate">
                          {preset.label}
                        </div>
                        <div className="text-xs text-[var(--ink-muted)] tabular-nums">
                          {preset.merchant} · {formatCents(preset.amountCents, selectedCurrency)}
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={() => onDeletePreset(preset.id)}
                        aria-label="Delete preset"
                        className="w-8 h-8 rounded-full hover:bg-[var(--coral-container)]/40 text-[var(--ink-muted)] hover:text-[var(--coral-tertiary)] flex items-center justify-center cursor-pointer"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
              <button
                type="button"
                onClick={onAddPresetClicked}
                className="rounded-full border border-[var(--hairline)] px-4 py-2 text-xs font-bold text-[var(--ink-primary)] hover:border-[var(--mint-primary)]/45 cursor-pointer"
              >
                Add recurring shortcut
              </button>
            </CrispCard>
          </div>

          {/* Custom Store Rules */}
          <div className="space-y-2">
            <div className="font-mono-data text-[10.5px] font-bold tracking-[0.08em] uppercase text-[var(--ink-muted)] px-1">
              CUSTOM STORE RULES
            </div>
            <CrispCard className="p-4 sm:p-5 space-y-3">
              {snapshot.merchantRules.length === 0 ? (
                <p className="text-xs text-[var(--ink-muted)] leading-relaxed">
                  No custom store overrides yet. Common chains and stores are matched automatically.
                </p>
              ) : (
                <div className="space-y-2">
                  {snapshot.merchantRules.map((rule) => (
                    <div key={rule.id} className="flex items-center justify-between gap-2">
                      <div>
                        <div className="text-sm font-medium text-[var(--ink-primary)]">
                          {rule.merchantKeyword}
                        </div>
                        <div className="text-xs text-[var(--ink-muted)]">
                          {CATEGORY_META[rule.category].displayName}
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={() => onDeleteMerchantRule(rule.id)}
                        aria-label="Delete store rule"
                        className="w-8 h-8 rounded-full hover:bg-[var(--coral-container)]/40 text-[var(--ink-muted)] hover:text-[var(--coral-tertiary)] flex items-center justify-center cursor-pointer"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              )}
              <button
                type="button"
                onClick={() => setShowAddRuleModal(true)}
                className="rounded-full border border-[var(--hairline)] px-4 py-2 text-xs font-bold text-[var(--ink-primary)] hover:border-[var(--mint-primary)]/45 cursor-pointer"
              >
                Add store rule
              </button>
            </CrispCard>
          </div>
        </>
      )}

      {/* App Updates (GitHub Releases) */}
      <div className="space-y-2">
        <div className="font-mono-data text-[10.5px] font-bold tracking-[0.08em] uppercase text-[var(--ink-muted)] px-1">
          APP UPDATES (GITHUB RELEASES)
        </div>
        <CrispCard className="p-4 sm:p-5 space-y-3">
          <div className="text-xs text-[var(--ink-muted)]">
            Installed version: <strong className="text-[var(--ink-primary)]">v{APP_VERSION_NAME}</strong>
          </div>
          <div>
            <label className="block text-xs font-semibold text-[var(--ink-muted)] mb-1">
              GitHub repository (owner/repo)
            </label>
            <input
              type="text"
              value={githubRepoInput}
              maxLength={LIMITS.GITHUB_REPO_MAX_LEN}
              onChange={(e) => setGithubRepoInput(e.target.value)}
              placeholder="e.g. gianrufin/SageLedger"
              data-testid="github_repo_input"
              className="w-full rounded-2xl bg-[var(--bg-surface-low)] border border-[var(--hairline)] px-3.5 py-2.5 text-sm text-[var(--ink-primary)]"
            />
          </div>
          <button
            type="button"
            onClick={async () => {
              await onSaveFinanceSettings({
                monthlyIncomeCents: parseToCents(incomeInput),
                monthlyBudgetCents: parseToCents(budgetInput),
                currencyCode: selectedCurrency,
                githubRepo: githubRepoInput,
              });
              await onCheckGitHubUpdate(githubRepoInput);
            }}
            data-testid="check_github_updates_button"
            className="rounded-full border border-[var(--hairline)] px-4 py-2 text-xs font-bold text-[var(--ink-primary)] hover:border-[var(--mint-primary)]/45 cursor-pointer"
          >
            Save &amp; check updates
          </button>
        </CrispCard>
      </div>

      {/* Google Account */}
      <div className="space-y-2">
        <div className="font-mono-data text-[10.5px] font-bold tracking-[0.08em] uppercase text-[var(--ink-muted)] px-1">
          GOOGLE ACCOUNT
        </div>
        <CrispCard className="p-4 sm:p-5 space-y-3">
          <div className="text-sm text-[var(--ink-primary)]">
            {userEmail || userName || 'Signed in'}
          </div>
          <button
            type="button"
            onClick={onSignOut}
            data-testid="sign_out_button"
            className="rounded-full border border-[var(--coral-tertiary)]/45 text-[var(--coral-tertiary)] px-4 py-2 text-xs font-bold hover:bg-[var(--coral-container)]/35 transition cursor-pointer"
          >
            Sign out
          </button>
        </CrispCard>
      </div>
    </div>
  );
}
