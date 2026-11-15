-- Catalogue category (e.g. WHOLE_SPICES). Optional so existing products stay valid.
alter table products add column category varchar(40);

create index ix_products_category on products (category);
