import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { api, ApiError, type Category, type MovementType, type StockLevel, type StockMovement } from "./api";
import { categoryLabel, formatAmount, formatChange, formatDateTime, formatNumber } from "./format";
import { Modal } from "./Modal";

/** Packs at or below this count are flagged as low. Display only; the backend has no reorder level yet. */
const LOW_STOCK_PACKS = 5;

type StatusFilter = "all" | "low" | "out";

function statusOf(level: StockLevel): "out" | "low" | "ok" {
  if (level.quantity === 0) {
    return "out";
  }
  return level.quantity <= LOW_STOCK_PACKS ? "low" : "ok";
}

export function StockPage({ categories }: { categories: Category[] }) {
  const [levels, setLevels] = useState<StockLevel[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [showInactive, setShowInactive] = useState(false);
  const [movement, setMovement] = useState<{ level: StockLevel; type: MovementType } | null>(null);
  const [history, setHistory] = useState<StockLevel | null>(null);

  const load = useCallback(() => {
    api.stock().then(
      (loaded) => {
        setLevels(loaded);
        setError(null);
      },
      (failure: unknown) => setError(failure instanceof Error ? failure.message : "Could not load stock"),
    );
  }, []);

  useEffect(load, [load]);

  const active = useMemo(() => (levels ?? []).filter((level) => level.active), [levels]);
  const outCount = active.filter((level) => statusOf(level) === "out").length;
  const lowCount = active.filter((level) => statusOf(level) === "low").length;
  const totalPacks = active.reduce((sum, level) => sum + level.quantity, 0);

  const visible = useMemo(() => {
    const term = search.trim().toLowerCase();
    return (levels ?? []).filter(
      (level) =>
        (showInactive || level.active) &&
        (category === "" || level.category === category) &&
        (status === "all" || statusOf(level) === status) &&
        (term === "" || level.sku.toLowerCase().includes(term) || level.name.toLowerCase().includes(term)),
    );
  }, [levels, search, category, status, showInactive]);

  return (
    <>
      <section className="tiles" aria-label="Stock summary">
        <Tile label="Active products" value={formatNumber(active.length)} />
        <Tile label="Packs on hand" value={formatNumber(totalPacks)} />
        <Tile label={`Low stock (${LOW_STOCK_PACKS} packs or fewer)`} value={formatNumber(lowCount)} tone="warning" />
        <Tile label="Out of stock" value={formatNumber(outCount)} tone="danger" />
      </section>

      <section className="toolbar">
        <input
          type="search"
          placeholder="Search by SKU or name"
          aria-label="Search stock"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <select aria-label="Category" value={category} onChange={(event) => setCategory(event.target.value)}>
          <option value="">All categories</option>
          {categories.map((option) => (
            <option key={option.code} value={option.code}>
              {option.label}
            </option>
          ))}
        </select>
        <select
          aria-label="Stock status"
          value={status}
          onChange={(event) => setStatus(event.target.value as StatusFilter)}
        >
          <option value="all">Any stock level</option>
          <option value="low">Low stock</option>
          <option value="out">Out of stock</option>
        </select>
        <label className="checkbox">
          <input type="checkbox" checked={showInactive} onChange={(event) => setShowInactive(event.target.checked)} />
          Show inactive
        </label>
      </section>

      {error && (
        <div className="banner banner-error" role="alert">
          {error}{" "}
          <button type="button" className="link-button" onClick={load}>
            Retry
          </button>
        </div>
      )}

      {levels === null && !error && <p className="muted">Loading stock…</p>}

      {levels !== null && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>SKU</th>
                <th>Product</th>
                <th>Category</th>
                <th className="num">Pack</th>
                <th className="num">On hand (packs)</th>
                <th className="num">Total</th>
                <th>Updated</th>
                <th aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {visible.map((level) => (
                <tr key={level.productId} className={level.active ? undefined : "inactive"}>
                  <td className="mono">{level.sku}</td>
                  <td>
                    {level.name}
                    {!level.active && <span className="badge badge-muted">Inactive</span>}
                  </td>
                  <td>{categoryLabel(categories, level.category)}</td>
                  <td className="num">{formatAmount(level.packSize, level.unit)}</td>
                  <td className="num">
                    <strong>{formatNumber(level.quantity)}</strong>
                    {statusOf(level) === "out" && <span className="badge badge-danger">Out</span>}
                    {statusOf(level) === "low" && <span className="badge badge-warning">Low</span>}
                  </td>
                  <td className="num">{formatAmount(level.totalAmount, level.unit)}</td>
                  <td className="muted">{formatDateTime(level.updatedAt)}</td>
                  <td className="actions">
                    <button
                      type="button"
                      className="button button-small button-positive"
                      disabled={!level.active}
                      title={level.active ? undefined : "Inactive products only accept adjustments"}
                      onClick={() => setMovement({ level, type: "RECEIVE" })}
                    >
                      Receive
                    </button>
                    <button
                      type="button"
                      className="button button-small"
                      disabled={!level.active || level.quantity === 0}
                      onClick={() => setMovement({ level, type: "SELL" })}
                    >
                      Sell
                    </button>
                    <button
                      type="button"
                      className="button button-small button-quiet"
                      onClick={() => setMovement({ level, type: "ADJUST" })}
                    >
                      Adjust
                    </button>
                    <button type="button" className="button button-small button-quiet" onClick={() => setHistory(level)}>
                      History
                    </button>
                  </td>
                </tr>
              ))}
              {visible.length === 0 && (
                <tr>
                  <td colSpan={8} className="empty">
                    {levels.length === 0
                      ? "No products yet. Add one on the Products tab."
                      : "No products match these filters."}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {movement && (
        <MovementDialog
          level={movement.level}
          initialType={movement.type}
          onClose={() => setMovement(null)}
          onRecorded={() => {
            setMovement(null);
            load();
          }}
        />
      )}
      {history && <HistoryDialog level={history} onClose={() => setHistory(null)} />}
    </>
  );
}

function Tile({ label, value, tone }: { label: string; value: string; tone?: "warning" | "danger" }) {
  return (
    <div className={tone ? `tile tile-${tone}` : "tile"}>
      <div className="tile-value">{value}</div>
      <div className="tile-label">{label}</div>
    </div>
  );
}

const MOVEMENT_LABEL: Record<MovementType, string> = {
  RECEIVE: "Receive",
  SELL: "Sell",
  ADJUST: "Adjust",
};

function MovementDialog({
  level,
  initialType,
  onClose,
  onRecorded,
}: {
  level: StockLevel;
  initialType: MovementType;
  onClose: () => void;
  onRecorded: () => void;
}) {
  const [type, setType] = useState<MovementType>(initialType);
  const [quantity, setQuantity] = useState("");
  const [reference, setReference] = useState("");
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);

  const parsed = Number(quantity);
  const validNumber = quantity.trim() !== "" && Number.isInteger(parsed);
  const change = !validNumber ? null : type === "RECEIVE" ? parsed : type === "SELL" ? -parsed : parsed;
  const balance = change === null ? null : level.quantity + change;

  let problem: string | null = null;
  if (quantity.trim() !== "") {
    if (!validNumber) {
      problem = "Enter a whole number of packs.";
    } else if (type !== "ADJUST" && parsed <= 0) {
      problem = "Enter a quantity greater than 0.";
    } else if (type === "ADJUST" && parsed === 0) {
      problem = "An adjustment cannot be 0.";
    } else if (balance !== null && balance < 0) {
      problem = `Only ${formatNumber(level.quantity)} packs on hand.`;
    }
  }
  const canSubmit = validNumber && problem === null && !saving;

  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (!canSubmit) {
      return;
    }
    setSaving(true);
    setError(null);
    api
      .recordMovement(level.productId, {
        type,
        quantity: parsed,
        reference: reference.trim() || null,
        note: note.trim() || null,
      })
      .then(onRecorded, (failure: unknown) => {
        setSaving(false);
        setError(failure instanceof ApiError ? failure : new ApiError(0, "ERROR", "Could not record the movement"));
      });
  };

  const allowedTypes: MovementType[] = level.active ? ["RECEIVE", "SELL", "ADJUST"] : ["ADJUST"];

  return (
    <Modal title={`${MOVEMENT_LABEL[type]} stock`} onClose={onClose}>
      <form onSubmit={submit} className="form">
        <p className="form-subject">
          <span className="mono">{level.sku}</span> · {level.name}
          <br />
          <span className="muted">
            {formatNumber(level.quantity)} packs on hand ({formatAmount(level.packSize, level.unit)} each)
          </span>
        </p>

        <div className="segmented" role="group" aria-label="Movement type">
          {allowedTypes.map((option) => (
            <button
              key={option}
              type="button"
              className={option === type ? "segment active" : "segment"}
              onClick={() => setType(option)}
            >
              {MOVEMENT_LABEL[option]}
            </button>
          ))}
        </div>

        <label>
          {type === "ADJUST" ? "Change in packs (use a minus sign to remove)" : "Packs"}
          <input
            type="number"
            step="1"
            inputMode="numeric"
            autoFocus
            required
            value={quantity}
            onChange={(event) => setQuantity(event.target.value)}
          />
          {(problem ?? error?.fieldMessage("quantity")) && (
            <span className="field-error">{problem ?? error?.fieldMessage("quantity")}</span>
          )}
        </label>

        <label>
          Reference <span className="muted">(optional, e.g. PO or invoice number)</span>
          <input type="text" maxLength={100} value={reference} onChange={(event) => setReference(event.target.value)} />
        </label>

        <label>
          Note <span className="muted">(optional)</span>
          <textarea rows={2} maxLength={500} value={note} onChange={(event) => setNote(event.target.value)} />
        </label>

        {balance !== null && problem === null && (
          <p className="preview">
            New balance: <strong>{formatNumber(balance)}</strong> packs ({formatChange(change ?? 0)})
          </p>
        )}

        {error && error.fieldErrors.length === 0 && (
          <div className="banner banner-error" role="alert">
            {error.message}
          </div>
        )}

        <footer className="form-actions">
          <button type="button" className="button button-quiet" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="button button-primary" disabled={!canSubmit}>
            {saving ? "Saving…" : MOVEMENT_LABEL[type]}
          </button>
        </footer>
      </form>
    </Modal>
  );
}

function HistoryDialog({ level, onClose }: { level: StockLevel; onClose: () => void }) {
  const [movements, setMovements] = useState<StockMovement[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.movements(level.productId).then(setMovements, (failure: unknown) =>
      setError(failure instanceof Error ? failure.message : "Could not load history"),
    );
  }, [level.productId]);

  return (
    <Modal title="Stock history" onClose={onClose} wide>
      <div className="modal-body">
        <p className="form-subject">
          <span className="mono">{level.sku}</span> · {level.name}
        </p>
        {error && (
          <div className="banner banner-error" role="alert">
            {error}
          </div>
        )}
        {movements === null && !error && <p className="muted">Loading…</p>}
        {movements !== null && movements.length === 0 && <p className="muted">No movements recorded yet.</p>}
        {movements !== null && movements.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>When</th>
                  <th>Type</th>
                  <th className="num">Change</th>
                  <th className="num">Balance</th>
                  <th>Reference</th>
                  <th>Note</th>
                </tr>
              </thead>
              <tbody>
                {movements.map((movement) => (
                  <tr key={movement.id}>
                    <td>{formatDateTime(movement.occurredAt)}</td>
                    <td>{MOVEMENT_LABEL[movement.type]}</td>
                    <td className={movement.quantityChange > 0 ? "num positive" : "num negative"}>
                      {formatChange(movement.quantityChange)}
                    </td>
                    <td className="num">{formatNumber(movement.balanceAfter)}</td>
                    <td>{movement.reference ?? ""}</td>
                    <td>{movement.note ?? ""}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {movements.length === 100 && <p className="muted">Showing the 100 most recent movements.</p>}
          </div>
        )}
      </div>
    </Modal>
  );
}
