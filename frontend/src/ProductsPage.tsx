import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { api, ApiError, UNITS, type Category, type Product, type ProductInput, type Unit } from "./api";
import { categoryLabel, formatAmount, unitName } from "./format";
import { Modal } from "./Modal";

const SKU_PATTERN = /^[A-Z0-9][A-Z0-9-]{1,39}$/;

function toInput(product: Product, overrides: Partial<ProductInput> = {}): ProductInput {
  return {
    sku: product.sku,
    name: product.name,
    description: product.description,
    category: product.category,
    unit: product.unit,
    packSize: product.packSize,
    active: product.active,
    ...overrides,
  };
}

export function ProductsPage({ categories }: { categories: Category[] }) {
  const [products, setProducts] = useState<Product[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("");
  const [showInactive, setShowInactive] = useState(true);
  const [editing, setEditing] = useState<Product | "new" | null>(null);

  const load = useCallback(() => {
    api.products().then(
      (loaded) => {
        setProducts(loaded);
        setError(null);
      },
      (failure: unknown) => setError(failure instanceof Error ? failure.message : "Could not load products"),
    );
  }, []);

  useEffect(load, [load]);

  const visible = useMemo(() => {
    const term = search.trim().toLowerCase();
    return (products ?? []).filter(
      (product) =>
        (showInactive || product.active) &&
        (category === "" || product.category === category) &&
        (term === "" || product.sku.toLowerCase().includes(term) || product.name.toLowerCase().includes(term)),
    );
  }, [products, search, category, showInactive]);

  const run = (action: Promise<unknown>) => {
    action.then(load, (failure: unknown) =>
      setError(failure instanceof Error ? failure.message : "The change could not be saved"),
    );
  };

  const toggleActive = (product: Product) =>
    run(api.updateProduct(product.id, toInput(product, { active: !product.active })));

  const remove = (product: Product) => {
    if (window.confirm(`Delete ${product.sku} (${product.name})? This cannot be undone.`)) {
      run(api.deleteProduct(product.id));
    }
  };

  return (
    <>
      <section className="toolbar">
        <input
          type="search"
          placeholder="Search by SKU or name"
          aria-label="Search products"
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
        <label className="checkbox">
          <input type="checkbox" checked={showInactive} onChange={(event) => setShowInactive(event.target.checked)} />
          Show inactive
        </label>
        <span className="spacer" />
        <button type="button" className="button button-primary" onClick={() => setEditing("new")}>
          New product
        </button>
      </section>

      {error && (
        <div className="banner banner-error" role="alert">
          {error}{" "}
          <button type="button" className="link-button" onClick={() => setError(null)}>
            Dismiss
          </button>
        </div>
      )}

      {products === null && !error && <p className="muted">Loading products…</p>}

      {products !== null && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>SKU</th>
                <th>Name</th>
                <th>Category</th>
                <th className="num">Pack</th>
                <th>Status</th>
                <th aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {visible.map((product) => (
                <tr key={product.id} className={product.active ? undefined : "inactive"}>
                  <td className="mono">{product.sku}</td>
                  <td>
                    {product.name}
                    {product.description && <div className="muted small">{product.description}</div>}
                  </td>
                  <td>{categoryLabel(categories, product.category)}</td>
                  <td className="num">{formatAmount(product.packSize, product.unit)}</td>
                  <td>
                    {product.active ? (
                      <span className="badge badge-positive">Active</span>
                    ) : (
                      <span className="badge badge-muted">Inactive</span>
                    )}
                  </td>
                  <td className="actions">
                    <button type="button" className="button button-small" onClick={() => setEditing(product)}>
                      Edit
                    </button>
                    <button
                      type="button"
                      className="button button-small button-quiet"
                      onClick={() => toggleActive(product)}
                    >
                      {product.active ? "Deactivate" : "Activate"}
                    </button>
                    <button type="button" className="button button-small button-danger" onClick={() => remove(product)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
              {visible.length === 0 && (
                <tr>
                  <td colSpan={6} className="empty">
                    {products.length === 0 ? "No products yet." : "No products match these filters."}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {editing && (
        <ProductDialog
          product={editing === "new" ? null : editing}
          categories={categories}
          onClose={() => setEditing(null)}
          onSaved={() => {
            setEditing(null);
            load();
          }}
        />
      )}
    </>
  );
}

function ProductDialog({
  product,
  categories,
  onClose,
  onSaved,
}: {
  product: Product | null;
  categories: Category[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [sku, setSku] = useState(product?.sku ?? "");
  const [name, setName] = useState(product?.name ?? "");
  const [description, setDescription] = useState(product?.description ?? "");
  const [category, setCategory] = useState(product?.category ?? "");
  const [unit, setUnit] = useState<Unit>(product?.unit ?? "GRAM");
  const [packSize, setPackSize] = useState(product ? String(product.packSize) : "");
  const [active, setActive] = useState(product?.active ?? true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<ApiError | null>(null);

  const skuProblem = sku !== "" && !SKU_PATTERN.test(sku) ? "2–40 characters: A–Z, 0–9 and hyphens." : undefined;
  const size = Number(packSize);
  const sizeProblem = packSize !== "" && !(size > 0) ? "Must be greater than 0." : undefined;

  const submit = (event: FormEvent) => {
    event.preventDefault();
    if (skuProblem || sizeProblem || saving) {
      return;
    }
    const input: ProductInput = {
      sku,
      name: name.trim(),
      description: description.trim() || null,
      category: category || null,
      unit,
      packSize: size,
      active,
    };
    setSaving(true);
    setError(null);
    const saved = product ? api.updateProduct(product.id, input) : api.createProduct(input);
    saved.then(onSaved, (failure: unknown) => {
      setSaving(false);
      setError(failure instanceof ApiError ? failure : new ApiError(0, "ERROR", "Could not save the product"));
    });
  };

  const duplicateSku = error?.code === "DUPLICATE_SKU" ? error.message : undefined;

  return (
    <Modal title={product ? "Edit product" : "New product"} onClose={onClose}>
      <form onSubmit={submit} className="form">
        <label>
          SKU
          <input
            type="text"
            className="mono"
            required
            autoFocus={!product}
            maxLength={40}
            placeholder="TUR-100G"
            value={sku}
            onChange={(event) => {
              setSku(event.target.value.toUpperCase());
              setError(null); // a server-side SKU error no longer applies once the SKU changes
            }}
          />
          <FieldError message={skuProblem ?? duplicateSku ?? error?.fieldMessage("sku")} />
        </label>

        <label>
          Name
          <input type="text" required maxLength={200} value={name} onChange={(event) => setName(event.target.value)} />
          <FieldError message={error?.fieldMessage("name")} />
        </label>

        <label>
          Category
          <select value={category} onChange={(event) => setCategory(event.target.value)}>
            <option value="">Uncategorised</option>
            {categories.map((option) => (
              <option key={option.code} value={option.code}>
                {option.label}
              </option>
            ))}
          </select>
        </label>

        <div className="form-row">
          <label>
            Pack size
            <input
              type="number"
              required
              min="0"
              step="any"
              value={packSize}
              onChange={(event) => setPackSize(event.target.value)}
            />
            <FieldError message={sizeProblem ?? error?.fieldMessage("packSize")} />
          </label>
          <label>
            Unit
            <select value={unit} onChange={(event) => setUnit(event.target.value as Unit)}>
              {UNITS.map((option) => (
                <option key={option} value={option}>
                  {unitName(option)}
                </option>
              ))}
            </select>
          </label>
        </div>

        <label>
          Description <span className="muted">(optional)</span>
          <textarea
            rows={2}
            maxLength={2000}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
          <FieldError message={error?.fieldMessage("description")} />
        </label>

        <label className="checkbox">
          <input type="checkbox" checked={active} onChange={(event) => setActive(event.target.checked)} />
          Active (can be received and sold)
        </label>

        {error && error.fieldErrors.length === 0 && !duplicateSku && (
          <div className="banner banner-error" role="alert">
            {error.message}
          </div>
        )}

        <footer className="form-actions">
          <button type="button" className="button button-quiet" onClick={onClose}>
            Cancel
          </button>
          <button type="submit" className="button button-primary" disabled={saving}>
            {saving ? "Saving…" : product ? "Save changes" : "Create product"}
          </button>
        </footer>
      </form>
    </Modal>
  );
}

function FieldError({ message }: { message: string | undefined }) {
  return message ? <span className="field-error">{message}</span> : null;
}
