package hit_ucc.behaviour.dictionary;

public interface IColumn {
	int getValue(int index);
	void setValue(int index, int value);
	int size();
}
