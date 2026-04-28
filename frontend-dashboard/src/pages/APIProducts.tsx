import React, { useState, useEffect } from 'react';
import { Package, ShoppingCart, Tag, Search, Filter } from 'lucide-react';
import { cn } from '../lib/utils';

export const APIProducts = () => {
  const [products, setProducts] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // In a real app, this would fetch from http://localhost:8080/api/v1/products
    // For now, we use high-quality mock data
    setTimeout(() => {
      setProducts([
        { id: 1, name: 'iPhone 15 Pro', price: 999.99, category: 'Electronics', stock: 45, icon: '📱' },
        { id: 2, name: 'MacBook Air M2', price: 1199.00, category: 'Computing', stock: 12, icon: '💻' },
        { id: 3, name: 'AirPods Pro', price: 249.00, category: 'Audio', stock: 89, icon: '🎧' },
        { id: 4, name: 'Apple Watch Ultra', price: 799.00, category: 'Wearables', stock: 23, icon: '⌚' },
        { id: 5, name: 'iPad Pro', price: 899.00, category: 'Tablets', stock: 34, icon: '平板' },
      ]);
      setLoading(false);
    }, 1000);
  }, []);

  return (
    <div className="space-y-8 animate-slide-up">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight text-card-foreground">API Products</h1>
          <p className="text-muted-foreground mt-1">Direct view of the data served by your endpoints.</p>
        </div>
        <button className="btn btn-primary gap-2">
          <Package className="h-4 w-4" />
          Add Product
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {loading ? (
          [1, 2, 3].map(i => <div key={i} className="card h-64 animate-pulse bg-muted/50" />)
        ) : (
          products.map((product) => (
            <div key={product.id} className="card group p-6 hover:border-primary/50 cursor-pointer overflow-hidden relative">
              <div className="absolute top-0 right-0 p-8 opacity-5 group-hover:opacity-10 transition-opacity">
                <Package className="h-24 w-24" />
              </div>
              <div className="flex items-start justify-between">
                <div className="text-4xl">{product.icon}</div>
                <div className="badge bg-primary/10 text-primary">{product.category}</div>
              </div>
              <div className="mt-6">
                <h3 className="text-xl font-bold text-card-foreground">{product.name}</h3>
                <div className="flex items-center gap-2 mt-2 text-muted-foreground">
                  <Tag className="h-4 w-4" />
                  <span className="text-sm">Product ID: #{product.id}</span>
                </div>
              </div>
              <div className="mt-6 pt-6 border-t border-border flex items-center justify-between">
                <div className="text-2xl font-bold text-primary">
                  ${product.price.toLocaleString()}
                </div>
                <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground bg-muted px-2 py-1 rounded">
                  <ShoppingCart className="h-3 w-3" />
                  {product.stock} in stock
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
