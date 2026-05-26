package com.krishna.crimedetection.models;

public class PaginationState {
    private int currentPage;
    private int pageSize;
    private int totalCount;
    private boolean hasMore;

    public PaginationState() {
        this.currentPage = 0;
        this.pageSize = 10;
        this.totalCount = 0;
        this.hasMore = true;
    }

    public int getSkip() {
        return currentPage * pageSize;
    }

    public int getLimit() {
        return pageSize;
    }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
        this.hasMore = (getSkip() + pageSize) < totalCount;
    }

    public boolean hasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }

    public void reset() {
        this.currentPage = 0;
        this.hasMore = true;
    }

    public void nextPage() {
        this.currentPage++;
    }
}
