package com.bambina.dashboardViewer.item;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.text.Html;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.bambina.dashboardViewer.DimensionHelper;
import com.bambina.dashboardViewer.R;
import com.bambina.dashboardViewer.activity.MainActivity;
import com.bambina.dashboardViewer.widget.TableWidget;
import com.bambina.dashboardViewer.databinding.ItemTableBinding;
import com.xwray.groupie.Item;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by hirono-mayuko on 2017/04/24.
 */

public class TableWidgetItem extends Item<ItemTableBinding> {
    public String mWidgetId;
    private TableWidget mWidget;
    private MainActivity mainActivity;
    private Context mContext;

    public TableWidgetItem(String widgetId, HashMap<String, String> visualData, MainActivity activity) {
        super();
        mWidgetId = widgetId;
        mainActivity = activity;
        mContext = mainActivity.getContext();
        mWidget = new TableWidget(visualData, activity, this);
    }

    @Override
    public void bind(ItemTableBinding binding, int position) {
        binding.setTableWidget(mWidget);

        binding.errMsg.setVisibility(View.GONE);
        binding.title.setVisibility(View.GONE);
        binding.pager.setVisibility(View.GONE);
        binding.tabLayout.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);

        if (mWidget.getVisualName() == null) {
            return;
        }

        if(mWidget.isFailed){
            int layoutHeight = Math.round(DimensionHelper.convertDpToPx(mContext, 300f));
            binding.widgetWrapper.getLayoutParams().height = layoutHeight;
            binding.progressBar.setVisibility(View.GONE);
            binding.errMsg.setText(mContext.getResources().getString(R.string.data_parse_error));
            binding.errMsg.setVisibility(View.VISIBLE);
            return;
        }

        binding.widgetWrapper.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        binding.pager.setAdapter(new TablePagerAdapter());
        binding.tabLayout.setupWithViewPager(binding.pager, true);

        binding.progressBar.setVisibility(View.GONE);
        binding.title.setVisibility(View.VISIBLE);
        binding.pager.setVisibility(View.VISIBLE);
        binding.tabLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public int getLayout() {
        return R.layout.item_table;
    }

    public void notifyWidgetChanged() {
        mainActivity.notifyItemChanged(this);
    }

    private class TablePagerAdapter extends PagerAdapter {
        private int numPages;

        public TablePagerAdapter() {
            super();
            int size = mWidget.getData().size();
            if (size == 5) {
                numPages = 6;
            } else {
                numPages = size;
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (position == 5) {
                View v = inflater.inflate(R.layout.fragment_table_last_page, container, false);
                container.addView(v);
                return v;
            } else {
                View v = inflater.inflate(R.layout.fragment_table_page, container, false);
                container.addView(v);
                setTableData(v, position);
                return v;
            }
        }

        @Override
        public int getCount() {
            return numPages;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((LinearLayout) object);
        }

        private void setTableData(View v, int position) {
            TableLayout mTableView = (TableLayout) v.findViewById(R.id.gridView);
            ArrayList<ArrayList<String>> mPageData = mWidget.getPageData(position);

            int numColumn = mPageData.get(0).size();
            float columnWeight = 1f / numColumn;

            for (int p = 0; p < mPageData.size(); p++) {
                ArrayList<String> row = mPageData.get(p);
                TableRow tableRow = new TableRow(mContext);
                TableRow.LayoutParams params = new TableRow.LayoutParams(
                        TableRow.LayoutParams.MATCH_PARENT,
                        TableRow.LayoutParams.WRAP_CONTENT);
                tableRow.setLayoutParams(params);
                tableRow.setBackgroundColor(0xffeeeeee);
                tableRow.setPadding(0,1,0,0);

                // create a new linear layout to be added
                params = new TableRow.LayoutParams(0,
                        TableRow.LayoutParams.WRAP_CONTENT);
                LinearLayout layout = new LinearLayout(mContext);
                params.weight = 1;
                layout.setLayoutParams(params);
                layout.setWeightSum(1);

                for (int i = 0; i < row.size(); i++) {
                    String column = row.get(i);
                    TextView textView = new TextView(mContext);
                    if (p == 0) {
                        textView.setBackgroundColor(0xfff7f7f7);
                        textView.setTypeface(Typeface.DEFAULT_BOLD);
                        column = column.toUpperCase();
                    } else {
                        textView.setBackgroundColor(0xffffffff);
                    }
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
                    int padding = Math.round(DimensionHelper.convertDpToPx(mContext, 10f));
                    textView.setPadding(padding, padding, padding, padding);
                    LinearLayout.LayoutParams childParams = new LinearLayout.LayoutParams(0,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    childParams.weight = columnWeight;
                    childParams.setMargins(0, 0, 0, 1);
                    textView.setLayoutParams(childParams);
                    if (i == 0) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            textView.setText(Html.fromHtml(column, Html.FROM_HTML_MODE_LEGACY));
                        } else {
                            textView.setText(Html.fromHtml(column));
                        }
                        textView.setLinkTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
                    } else {
                        textView.setText(column);
                    }
                    textView.setMaxLines(1);
                    textView.setSingleLine(true);
                    textView.setEllipsize(TextUtils.TruncateAt.END);
                    layout.addView(textView);
                }
                tableRow.addView(layout);
                mTableView.addView(tableRow);
            }

        }
    }
}
