package java.bestpractice.singleinstance;

public class SingleInstance {
	private static volatile SingleInstance mInstance;
	
	private SingleInstance() {
		System.out.println("Private constructor SingleInstance");
	}
	
	public static SingleInstance getInstance() {
		if (mInstance == null) {
			synchronized(SingleInstance.class) {
				if (mInstance == null) {
					mInstance = new SingleInstance();
				}
			}
		}
		return mInstance;
	}
}
