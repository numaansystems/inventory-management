import { useEffect, useState } from "react";
import { api, type Category } from "./api";
import { ProductsPage } from "./ProductsPage";
import { StockPage } from "./StockPage";

type Tab = "stock" | "products";

function tabFromHash(): Tab {
  return window.location.hash === "#/products" ? "products" : "stock";
}

export function App() {
  const [tab, setTab] = useState<Tab>(tabFromHash);
  const [categories, setCategories] = useState<Category[]>([]);

  useEffect(() => {
    const onHashChange = () => setTab(tabFromHash());
    window.addEventListener("hashchange", onHashChange);
    return () => window.removeEventListener("hashchange", onHashChange);
  }, []);

  useEffect(() => {
    // If this fails the pages still work; they show their own error when the server is unreachable.
    api.categories().then(setCategories, () => setCategories([]));
  }, []);

  return (
    <div className="app">
      <header className="topbar">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true" />
          <span className="brand-name">Bumi Spices</span>
          <span className="brand-sub">Inventory</span>
        </div>
        <nav className="tabs" aria-label="Sections">
          <a href="#/stock" className={tab === "stock" ? "tab active" : "tab"}>
            Stock
          </a>
          <a href="#/products" className={tab === "products" ? "tab active" : "tab"}>
            Products
          </a>
        </nav>
      </header>
      <main className="content">
        {tab === "stock" ? <StockPage categories={categories} /> : <ProductsPage categories={categories} />}
      </main>
    </div>
  );
}
