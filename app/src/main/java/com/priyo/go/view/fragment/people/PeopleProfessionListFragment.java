package com.priyo.go.view.fragment.people;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.priyo.go.Api.ApiForbiddenResponse;
import com.priyo.go.Api.HttpRequestGetAsyncTask;
import com.priyo.go.Api.HttpResponseListener;
import com.priyo.go.Api.HttpResponseObject;
import com.priyo.go.Fragments.OnFragmentInteractionListener;
import com.priyo.go.Model.Business.CategoryNode;
import com.priyo.go.Model.api.graph.response.ProfessionListResponse;
import com.priyo.go.R;
import com.priyo.go.Utilities.ApiConstants;
import com.priyo.go.Utilities.Constants;
import com.priyo.go.controller.adapter.CategoryListController;
import com.priyo.go.view.adapter.RecyclerViewAdapter;
import com.priyo.go.view.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.priyo.go.view.activity.people.PeopleActivity.ID_PEOPLE_LIST_FRAGMENT;

/**
 * Created by sajid.shahriar on 5/3/17.
 */
public class PeopleProfessionListFragment extends Fragment implements HttpResponseListener, RecyclerView.OnItemClickListener {


    private final Gson gson = new GsonBuilder().create();
    private HttpRequestGetAsyncTask getProfessionCategoryTask;

    private List<CategoryNode> professionCategoryList;
    private RecyclerViewAdapter recyclerViewAdapter;
    private CategoryListController categoryListController;
    private View rootView;
    private RecyclerView peopleProfessionRecyclerView;

    private String accessToken;

    private ProgressDialog progressDialog;

    private OnFragmentInteractionListener onFragmentInteractionListener;

    public PeopleProfessionListFragment() {

    }

    public static PeopleProfessionListFragment newInstance(Bundle bundle) {
        PeopleProfessionListFragment peopleProfessionListFragment = new PeopleProfessionListFragment();
        peopleProfessionListFragment.setArguments(bundle);
        return peopleProfessionListFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(Constants.ApplicationTag, Context.MODE_PRIVATE);
        accessToken = sharedPreferences.getString(Constants.ACCESS_TOKEN_KEY, "");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_people_profession_list, container, false);
        }
        peopleProfessionRecyclerView = (RecyclerView) rootView.findViewById(R.id.people_profession_recycler_view);
        peopleProfessionRecyclerView.setAsListType(RecyclerView.VERTICAL_LAYOUT);
        peopleProfessionRecyclerView.setOnItemClickListener(this);

        professionCategoryList = new ArrayList<>();
        categoryListController = new CategoryListController(getActivity(), professionCategoryList);
        recyclerViewAdapter = new RecyclerViewAdapter(categoryListController);
        peopleProfessionRecyclerView.setAdapter(recyclerViewAdapter);
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Fetching Profession List" + "...");
        progressDialog.setCancelable(false);

        fetchPeopleProfessionList();
        return rootView;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            onFragmentInteractionListener = (OnFragmentInteractionListener) context;
        }
    }

    private void fetchPeopleProfessionList() {
        progressDialog.show();
        Map<String, String> params = new HashMap<>();
        params.put(ApiConstants.Parameter.PAGE_SIZE_PARAMETER, "all");
        params.put(ApiConstants.Parameter.SORTING_ORDER_PARAMETER, "asc");
        params.put(ApiConstants.Parameter.SORTING_PARAMETER, "name");

        getProfessionCategoryTask = new HttpRequestGetAsyncTask(ApiConstants.Command.PROFESSION_LIST_FETCH,
                ApiConstants.Url.SPIDER_API + ApiConstants.Module.GRAPH_MODULE + ApiConstants.EndPoint.PROFESSION_LIST_FETCH_ENDPOINT, getActivity(), params, this);
        getProfessionCategoryTask.addHeader(ApiConstants.Header.TOKEN, accessToken);
        getProfessionCategoryTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void httpResponseReceiver(@Nullable HttpResponseObject result) {
        progressDialog.cancel();
        getProfessionCategoryTask = null;
        if (result == null) {
            Toast.makeText(getContext(), R.string.service_not_available, Toast.LENGTH_SHORT).show();
            return;
        }

        switch (result.getStatus()) {
            case ApiConstants.StatusCode.S_OK:
            case ApiConstants.StatusCode.S_ACCEPTED:
                ProfessionListResponse professionListResponse = gson.fromJson(result.getJsonString(), ProfessionListResponse.class);
                professionCategoryList = professionListResponse.getData();
                categoryListController.updateItemList(professionCategoryList);
                recyclerViewAdapter.notifyDataSetChanged();
                break;
            case ApiConstants.StatusCode.S_UNAUTHORIZED:
            case ApiConstants.StatusCode.S_FORBIDDEN:
                ApiForbiddenResponse apiForbiddenResponse = gson.fromJson(result.getJsonString(), ApiForbiddenResponse.class);
                if (!TextUtils.isEmpty(apiForbiddenResponse.getDetail())) {
                    Toast.makeText(getContext(), apiForbiddenResponse.getDetail(), Toast.LENGTH_LONG).show();
                } else if (!TextUtils.isEmpty(apiForbiddenResponse.getMessage())) {
                    Toast.makeText(getContext(), apiForbiddenResponse.getMessage(), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), R.string.unauthorized, Toast.LENGTH_LONG).show();
                }
                break;
            case ApiConstants.StatusCode.S_NOT_FOUND:
            case ApiConstants.StatusCode.S_SERVER_ERROR:
                Toast.makeText(getContext(), R.string.service_not_available, Toast.LENGTH_LONG).show();
                break;
        }
    }

    @Override
    public boolean onItemClick(View clickedItemView, int position, long id) {
        if (position >= 0) {
            Bundle bundle = new Bundle();
            CategoryNode categoryNode = professionCategoryList.get(position);
            bundle.putString("fragmentActionType", ApiConstants.Parameter.RELATED_TO_ID_PARAMETER);
            bundle.putString(ApiConstants.Parameter.RELATED_TO_ID_PARAMETER, categoryNode.getNodeID());
            bundle.putString("Title", categoryNode.getNodeTitle());
            onFragmentInteractionListener.addToBackStack(ID_PEOPLE_LIST_FRAGMENT, bundle);
            return true;
        } else {
            return false;
        }
    }

}

