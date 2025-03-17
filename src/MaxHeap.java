import java.util.Comparator;

public class MaxHeap<T> {
    private List<T> heap;
    private Map<T, Integer> indices;
    private Comparator<T> comparator;

    public MaxHeap(Comparator<T> comparator) {
        heap = new ArrayList<>();
        indices = new SimpleMap<>();
        this.comparator = comparator;
    }

    public void insert(T item) {
        heap.add(heap.size(), item);
        int index = heap.size() - 1;
        indices.put(item, index);
        heapifyUp(index);
    }

    public T peek() {
        if (heap.isEmpty())
            return null;
        return heap.get(0);
    }

    public void remove(T item) {
        Integer index = indices.get(item);
        if (index == null)
            return;

        T lastItem = heap.remove(heap.size() - 1);
        indices.remove(item);

        if (index < heap.size()) {
            heap.set(index, lastItem);
            indices.put(lastItem, index);
            if (comparator.compare(lastItem, item) > 0) {
                heapifyUp(index);
            } else {
                heapifyDown(index);
            }
        }
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    private void heapifyUp(int index) {
        T item = heap.get(index);
        while (index > 0) {
            int parentIndex = (index - 1) / 2;
            T parent = heap.get(parentIndex);
            if (comparator.compare(item, parent) > 0) {
                heap.set(index, parent);
                indices.put(parent, index);
                index = parentIndex;
            } else {
                break;
            }
        }
        heap.set(index, item);
        indices.put(item, index);
    }

    private void heapifyDown(int index) {
        int size = heap.size();
        T item = heap.get(index);
        while (true) {
            int leftChildIdx = 2 * index + 1;
            int rightChildIdx = 2 * index + 2;
            int largestIdx = index;

            if (leftChildIdx < size && comparator.compare(heap.get(leftChildIdx), heap.get(largestIdx)) > 0) {
                largestIdx = leftChildIdx;
            }
            if (rightChildIdx < size && comparator.compare(heap.get(rightChildIdx), heap.get(largestIdx)) > 0) {
                largestIdx = rightChildIdx;
            }

            if (largestIdx != index) {
                T largestItem = heap.get(largestIdx);
                heap.set(index, largestItem);
                indices.put(largestItem, index);

                index = largestIdx;
            } else {
                break;
            }
        }
        heap.set(index, item);
        indices.put(item, index);
    }
}
