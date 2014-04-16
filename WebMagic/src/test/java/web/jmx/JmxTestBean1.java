package web.jmx;
import org.springframework.jmx.export.notification.NotificationPublisherAware;
import org.springframework.jmx.export.notification.NotificationPublisher;

import javax.management.Notification;

public class JmxTestBean1 implements IJmxTestBean, NotificationPublisherAware {

    private String name;
    private int age;
    private boolean isSuperman;
    private NotificationPublisher publisher;

    // other getters and setters omitted for clarity

    public int add(int x, int y) {
        int answer = x + y;
        this.publisher.sendNotification(new Notification("add", this, 0));
        return answer;
    }

    public void dontExposeMe() {
        throw new RuntimeException();
    }
    
    public void setNotificationPublisher(NotificationPublisher notificationPublisher) {
        this.publisher = notificationPublisher;
    }

	/**
	 * @return
	 */
	@Override
	public long myOperation() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @return
	 */
	@Override
	public int getAge() {
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * @param age
	 */
	@Override
	public void setAge(int age) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @param name
	 */
	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * @return
	 */
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
}