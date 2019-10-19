// import java.lang.reflect.Field;

// public class Node {
// 	public static abstract class ANode {
// 		// stackoverflow.com/questions/13128194/java-how-can-i-dynamically-reference-an-objects-property
// 		public void setField(String fieldName, Object value) {
// 			Field field;
// 			try {
// 				field = getClass().getDeclaredField(fieldName);
// 				field.set(this, value);
// 			} catch (Exception e) {
// 				e.printStackTrace();
// 			}
// 		}
// 	}

// 	public static class INode extends ANode {
// 		int high;
// 		int low;
// 		String value;

// 		INode(String value) {
// 			this.value = value;
// 		}
// 	}

// 	public static class ENode extends ANode {
// 		int value;

// 		ENode(int value) {
// 			this.value = value;
// 		}
// 	}
// }
