package com.example.oneorder_sm.data.pagination

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order as PostgrestOrder

class OrderPagingSource(
    private val postgrest: Postgrest,
    private val status: OrderStatus? = null
) : PagingSource<Int, Order>() {

    override fun getRefreshKey(state: PagingState<Int, Order>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Order> {
        return try {
            val page = params.key ?: 0
            val start = page.toLong() * params.loadSize
            val end = start + params.loadSize - 1

            val result = postgrest.from("orders").select {
                if (status != null) {
                    filter {
                        eq("status", status)
                    }
                }
                range(start, end)
                order("created_at", order = PostgrestOrder.DESCENDING)
            }
            
            val orders = result.decodeList<Order>()

            LoadResult.Page(
                data = orders,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (orders.isEmpty() || orders.size < params.loadSize) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
