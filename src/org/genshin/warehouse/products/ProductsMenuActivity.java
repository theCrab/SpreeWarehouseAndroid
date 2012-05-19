package org.genshin.warehouse.products;

import org.genshin.spree.SpreeConnector;
import org.genshin.warehouse.R;
import org.genshin.warehouse.Warehouse.ResultCodes;
import org.genshin.warehouse.products.ProductDetailsActivity;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

public class ProductsMenuActivity extends Activity {
	private static Products products;
	private static Product selectedProduct;
	private SpreeConnector spree;
	
	private ProductListAdapter productsAdapter;
	
	private ListView productList;
	private TextView statusText;
	private MultiAutoCompleteTextView searchBar;
	private Button searchButton;
	
	private Button clearButton;
	
	private ImageButton scanButton;

	private void initViewElements() {
        productList = (ListView) findViewById(R.id.product_menu_list);
        statusText = (TextView) findViewById(R.id.status_text);
        scanButton = (ImageButton) findViewById(R.id.products_menu_scan_button);
        searchBar = (MultiAutoCompleteTextView) findViewById(R.id.product_menu_searchbox);
		searchButton = (Button) findViewById(R.id.products_menu_search_button);
		clearButton = (Button) findViewById(R.id.products_menu_clear_button);
	}
	
	private void hookupInterface() {
        //Visual Code Scan hookup
		scanButton.setOnClickListener(new View.OnClickListener() {
        	public void onClick(View v) {
        		Toast.makeText(v.getContext(), getString(R.string.scan), Toast.LENGTH_LONG).show();
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                //intent.putExtra("SCAN_MODE", "BARCODE_MODE");
                startActivityForResult(intent, ResultCodes.SCAN.ordinal());
            }
		});
		
		//Standard text search hookup
		searchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				products.textSearch(searchBar.getText().toString());
				refreshProductMenu();
			}
		});
		
		//Clear button
		clearButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchBar.setText("");
				products.clear();
				refreshProductMenu();
			}
		});
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.products);

        initViewElements();
        hookupInterface();
        
        spree = new SpreeConnector(this);
        if (products == null) {
        	products = new Products(this, spree);
        	products.getNewestProducts(10);
        }
        
        refreshProductMenu();
	}

	public static enum menuCodes { registerProduct };

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		Resources res = getResources();
        // メニューアイテムを追加します
        menu.add(Menu.NONE, menuCodes.registerProduct.ordinal(), Menu.NONE, res.getString(R.string.register_product));
        return super.onCreateOptionsMenu(menu);
    }

	@Override
    public boolean onOptionsItemSelected(MenuItem item) {

		int id = item.getItemId();

		if (id == menuCodes.registerProduct.ordinal()) {
			Intent intent = new Intent(this, ProductNewActivity.class);
            startActivity(intent);
        	
			return true;
		}
        
        return false;
    }
	
	private void refreshProductMenu() {
		//Log.d("PRODUCTLIST", "length " + products.list.size());
		
		ProductListItem[] productListItems = new ProductListItem[products.list.size()];
		
		for (int i = 0; i < products.list.size(); i++) {
			Product p = products.list.get(i);
			Drawable thumb = null;
			if (p.images.size() > 0)
				thumb = p.images.get(0);
			
			productListItems[i] = new ProductListItem(thumb, p.name, p.sku, p.countOnHand, p.permalink, p.id);
		}
		
		statusText.setText(products.count + this.getString(R.string.products_counter) );
		
		productsAdapter = new ProductListAdapter(this, productListItems);
		productList.setAdapter(productsAdapter);
        productList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	productListClickHandler(parent, view, position);
            }
        });
        
	}
	
	public static void showProductDetails(Context ctx, Product product) {
		ProductsMenuActivity.setSelectedProduct(product);
		Intent productDetailsIntent = new Intent(ctx, ProductDetailsActivity.class);
    	ctx.startActivity(productDetailsIntent);
	}
	
	private void productListClickHandler(AdapterView<?> parent, View view, int position) {
		ProductsMenuActivity.showProductDetails(this, products.list.get(position));				
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == ResultCodes.SCAN.ordinal()) {
            if (resultCode == RESULT_OK) {
                String contents = intent.getStringExtra("SCAN_RESULT");
                String format = intent.getStringExtra("SCAN_RESULT_FORMAT");
                //TODO limit this to bar code types?
                if (format != "QR_CODE") {
                	//Assume barcode, and barcodes correlate to products
                	//Toast.makeText(this, "[" + format + "]: " + contents + "\nSearching!", Toast.LENGTH_LONG).show();
                	products.findByBarcode(contents);
                	//if we have one hit that's the product we want, so go to it
                	refreshProductMenu();
                	if (products.list.size() == 1)
                		showProductDetails(this, products.list.get(0));
                    
                	//Toast.makeText(this, "Results:" + products.count, Toast.LENGTH_LONG).show();
                }
                // Handle successful scan
            } else if (resultCode == RESULT_CANCELED) {
                // Handle cancel
            	Toast.makeText(this, "Scan Cancelled", Toast.LENGTH_LONG).show();
            }
        }
    }

	public static Product getSelectedProduct() {
		return selectedProduct;
	}

	public static void setSelectedProduct(Product selectedProduct) {
		if (selectedProduct == null)
			selectedProduct = new Product(0, "", "", 0, 0, "", "");
		
		ProductsMenuActivity.selectedProduct = selectedProduct;
	}
}
