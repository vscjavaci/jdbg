package jdwproxy;

public class JDWP {
	static class ClassStatus {
		public static final byte VERIFIED = 1;
		public static final byte PREPARED = 2;
		public static final byte INITIALIZED = 4;
		public static final byte ERROR = 8;
	}

	static class Command {
		// VirtualMachine Command Set
		public static final short VIRTUAL_MACHINE_VERSION = (1 << 8) + 1;
		public static final short VIRTUAL_MACHINE_CLASSES_BY_SIGNATURE = (1 << 8) + 2;
		public static final short VIRTUAL_MACHINE_ALL_CLASSES = (1 << 8) + 3;
		public static final short VIRTUAL_MACHINE_ALL_THREADS = (1 << 8) + 4;
		public static final short VIRTUAL_MACHINE_TOP_LEVEL_THREAD_GROUPS = (1 << 8) + 5;
		public static final short VIRTUAL_MACHINE_DISPOSE = (1 << 8) + 6;
		public static final short VIRTUAL_MACHINE_ID_SIZES = (1 << 8) + 7;
		public static final short VIRTUAL_MACHINE_SUSPEND = (1 << 8) + 8;
		public static final short VIRTUAL_MACHINE_RESUME = (1 << 8) + 9;
		public static final short VIRTUAL_MACHINE_EXIT = (1 << 8) + 10;
		public static final short VIRTUAL_MACHINE_CREATE_STRING = (1 << 8) + 11;
		public static final short VIRTUAL_MACHINE_CAPABILITIES = (1 << 8) + 12;
		public static final short VIRTUAL_MACHINE_CLASS_PATHS = (1 << 8) + 13;
		public static final short VIRTUAL_MACHINE_DISPOSE_OBJECTS = (1 << 8) + 14;
		public static final short VIRTUAL_MACHINE_HOLD_EVENTS = (1 << 8) + 15;
		public static final short VIRTUAL_MACHINE_RELEASE_EVENTS = (1 << 8) + 16;
		public static final short VIRTUAL_MACHINE_CAPABILITIES_NEW = (1 << 8) + 17;
		public static final short VIRTUAL_MACHINE_REDEFINE_CLASSES = (1 << 8) + 18;
		public static final short VIRTUAL_MACHINE_SET_DEFAULT_STRATUM = (1 << 8) + 19;
		public static final short VIRTUAL_MACHINE_ALL_CLASSES_WITH_GENERIC = (1 << 8) + 20;
		public static final short VIRTUAL_MACHINE_INSTANCE_COUNTS = (1 << 8) + 21;

		// ReferenceType Command Set
		public static final short REFERENCE_TYPE_SIGNATURE = (2 << 8) + 1;
		public static final short REFERENCE_TYPE_CLASS_LOADER = (2 << 8) + 2;
		public static final short REFERENCE_TYPE_MODIFIERS = (2 << 8) + 3;
		public static final short REFERENCE_TYPE_FIELDS = (2 << 8) + 4;
		public static final short REFERENCE_TYPE_METHODS = (2 << 8) + 5;
		public static final short REFERENCE_TYPE_GET_VALUES = (2 << 8) + 6;
		public static final short REFERENCE_TYPE_SOURCE_FILE = (2 << 8) + 7;
		public static final short REFERENCE_TYPE_NESTED_TYPES = (2 << 8) + 8;
		public static final short REFERENCE_TYPE_STATUS = (2 << 8) + 9;
		public static final short REFERENCE_TYPE_INTERFACES = (2 << 8) + 10;
		public static final short REFERENCE_TYPE_CLASS_OBJECT = (2 << 8) + 11;
		public static final short REFERENCE_TYPE_SOURCE_DEBUG_EXTENSIONS = (2 << 8) + 12;
		public static final short REFERENCE_TYPE_SIGNATURE_WITH_GENERIC = (2 << 8) + 13;
		public static final short REFERENCE_TYPE_FIELDS_WITH_GENERIC = (2 << 8) + 14;
		public static final short REFERENCE_TYPE_METHODS_WITH_GENERIC = (2 << 8) + 15;
		public static final short REFERENCE_TYPE_INSTANCES = (2 << 8) + 16;
		public static final short REFERENCE_TYPE_CLASS_FILE_VERSION = (2 << 8) + 17;
		public static final short REFERENCE_TYPE_CONSTANT_POOL = (2 << 8) + 18;

		// ClassType Command Set
		public static final short CLASS_TYPE_SUPERCLASS = (3 << 8) + 1;
		public static final short CLASS_TYPE_SET_VALUES = (3 << 8) + 2;
		public static final short CLASS_TYPE_INVOKE_METHOD = (3 << 8) + 3;
		public static final short CLASS_TYPE_NEW_INSTANCE = (3 << 8) + 4;

		// ArrayType Command Set
		public static final short ARRAY_TYPE_NEW_INSTANCE = (4 << 8) + 1;

		// InterfaceType Command Set

		// Method Command Set
		public static final short METHOD_LINE_TABLE = (6 << 8) + 1;
		public static final short METHOD_VARIABLE_TABLE = (6 << 8) + 2;
		public static final short METHOD_BYTECODES = (6 << 8) + 3;
		public static final short METHOD_IS_OBSOLETE = (6 << 8) + 4;
		public static final short METHOD_VARIABLE_TABLE_WITH_GENERIC = (6 << 8) + 5;

		// Field Command Set

		// ObjectReference Command Set
		public static final short OBJECT_REFERENCE_REFERENCE_TYPE = (9 << 8) + 1;
		public static final short OBJECT_REFERENCE_GET_VALUES = (9 << 8) + 2;
		public static final short OBJECT_REFERENCE_SET_VALUES = (9 << 8) + 3;
		public static final short OBJECT_REFERENCE_MONITOR_INFO = (9 << 8) + 5;
		public static final short OBJECT_REFERENCE_INVOKE_METHOD = (9 << 8) + 6;
		public static final short OBJECT_REFERENCE_DISABLE_COLLECTION = (9 << 8) + 7;
		public static final short OBJECT_REFERENCE_ENABLE_COLLECTION = (9 << 8) + 8;
		public static final short OBJECT_REFERENCE_IS_COLLECTED = (9 << 8) + 9;
		public static final short OBJECT_REFERENCE_REFERRING_OBJECTS = (9 << 8) + 10;

		// StringReference Command Set
		public static final short STRING_REFERENCE_VALUE = (10 << 8) + 1;

		// ThreadReference Command Set
		public static final short THREAD_REFERENCE_NAME = (11 << 8) + 1;
		public static final short THREAD_REFERENCE_SUSPEND = (11 << 8) + 2;
		public static final short THREAD_REFERENCE_RESUME = (11 << 8) + 3;
		public static final short THREAD_REFERENCE_STATUS = (11 << 8) + 4;
		public static final short THREAD_REFERENCE_THREAD_GROUP = (11 << 8) + 5;
		public static final short THREAD_REFERENCE_FRAMES = (11 << 8) + 6;
		public static final short THREAD_REFERENCE_FRAME_COUNT = (11 << 8) + 7;
		public static final short THREAD_REFERENCE_OWNED_MONITORS = (11 << 8) + 8;
		public static final short THREAD_REFERENCE_CURRENT_CONTENDED_MONITOR = (11 << 8) + 9;
		public static final short THREAD_REFERENCE_STOP = (11 << 8) + 10;
		public static final short THREAD_REFERENCE_INTERRUPT = (11 << 8) + 11;
		public static final short THREAD_REFERENCE_SUSPEND_COUNT = (11 << 8) + 12;
		public static final short THREAD_REFERENCE_OWNED_MONITORS_STACK_DEPTH_INFO = (11 << 8) + 13;
		public static final short THREAD_REFERENCE_FORCE_EARLY_RETURN = (11 << 8) + 14;

		// ThreadGroupReference Command Set
		public static final short THREAD_GROUP_REFERENCE_NAME = (12 << 8) + 1;
		public static final short THREAD_GROUP_REFERENCE_PARENT = (12 << 8) + 2;
		public static final short THREAD_GROUP_REFERENCE_CHILDREN = (12 << 8) + 3;

		// ArrayReference Command Set
		public static final short ARRAY_REFERENCE_LENGTH = (13 << 8) + 1;
		public static final short ARRAY_REFERENCE_GET_VALUES = (13 << 8) + 2;
		public static final short ARRAY_REFERENCE_SET_VALUES = (13 << 8) + 3;

		// ClassLoaderReference Command Set
		public static final short CLASS_LOADER_REFERENCE_VISIBLE_CLASSES = (14 << 8) + 1;

		// EventRequest Command Set
		public static final short EVENT_REQUEST_SET = (15 << 8) + 1;
		public static final short EVENT_REQUEST_CLEAR = (15 << 8) + 2;
		public static final short EVENT_REQUEST_CLEAR_ALL_BREAKPOINTS = (15 << 8) + 3;

		// StackFrame Command Set
		public static final short STACK_FRAME_GET_VALUES = (16 << 8) + 1;
		public static final short STACK_FRAME_SET_VALUES = (16 << 8) + 2;
		public static final short STACK_FRAME_THIS_OBJECT = (16 << 8) + 3;
		public static final short STACK_FRAME_POP_FRAMES = (16 << 8) + 4;

		// ClassObjectReference Command Set
		public static final short CLASS_OBJECT_REFERENCE_REFLECTED_TYPE = (17 << 8) + 1;

		// Event Command Set
		public static final short EVENT_COMPOSITE = (64 << 8) + 100;
	}

	static class Error {
		public static final byte NONE = 0;
		public static final byte INVALID_THREAD = 10;
		public static final byte INVALID_THREAD_GROUP = 11;
		public static final byte INVALID_PRIORITY = 12;
		public static final byte THREAD_NOT_SUSPENDED = 13;
		public static final byte THREAD_SUSPENDED = 14;
		public static final byte THREAD_NOT_ALIVE = 15;
		public static final byte INVALID_OBJECT = 20;
		public static final byte INVALID_CLASS = 21;
		public static final byte CLASS_NOT_PREPARED = 22;
		public static final byte INVALID_METHODID = 23;
		public static final byte INVALID_LOCATION = 24;
		public static final byte INVALID_FIELDID = 25;
		public static final byte INVALID_FRAMEID = 30;
		public static final byte NO_MORE_FRAMES = 31;
		public static final byte OPAQUE_FRAME = 32;
		public static final byte NOT_CURRENT_FRAME = 33;
		public static final byte TYPE_MISMATCH = 34;
		public static final byte INVALID_SLOT = 35;
		public static final byte DUPLICATE = 40;
		public static final byte NOT_FOUND = 41;
		public static final byte INVALID_MONITOR = 50;
		public static final byte NOT_MONITOR_OWNER = 51;
		public static final byte INTERRUPT = 52;
		public static final byte INVALID_CLASS_FORMAT = 60;
		public static final byte CIRCULAR_CLASS_DEFINITION = 61;
		public static final byte FAILS_VERIFICATION = 62;
		public static final byte ADD_METHOD_NOT_IMPLEMENTED = 63;
		public static final byte SCHEMA_CHANGE_NOT_IMPLEMENTED = 64;
		public static final byte INVALID_TYPESTATE = 65;
		public static final byte HIERARCHY_CHANGE_NOT_IMPLEMENTED = 66;
		public static final byte DELETE_METHOD_NOT_IMPLEMENTED = 67;
		public static final byte UNSUPPORTED_VERSION = 68;
		public static final byte NAMES_DONT_MATCH = 69;
		public static final byte CLASS_MODIFIERS_CHANGE_NOT_IMPLEMENTED = 70;
		public static final byte METHOD_MODIFIERS_CHANGE_NOT_IMPLEMENTED = 71;
		public static final byte NOT_IMPLEMENTED = 99;
		public static final byte NULL_POINTER = 100;
		public static final byte ABSENT_INFORMATION = 101;
		public static final byte INVALID_EVENT_TYPE = 102;
		public static final byte ILLEGAL_ARGUMENT = 103;
		public static final byte OUT_OF_MEMORY = 110;
		public static final byte ACCESS_DENIED = 111;
		public static final byte VM_DEAD = 112;
		public static final byte INTERNAL = 113;
		public static final byte UNATTACHED_THREAD = 115;
		public static final byte INVALID_TAG = (byte)500;
		public static final byte ALREADY_INVOKING = (byte)502;
		public static final byte INVALID_INDEX = (byte)503;
		public static final byte INVALID_LENGTH = (byte)504;
		public static final byte INVALID_STRING = (byte)506;
		public static final byte INVALID_CLASS_LOADER = (byte)507;
		public static final byte INVALID_ARRAY = (byte)508;
		public static final byte TRANSPORT_LOAD = (byte)509;
		public static final byte TRANSPORT_INIT = (byte)510;
		public static final byte NATIVE_METHOD = (byte)511;
		public static final byte INVALID_COUNT = (byte)512;
	}

	static class EventKind {
		public static final byte SINGLE_STEP = 1;
		public static final byte BREAKPOINT = 2;
		public static final byte FRAME_POP = 3;
		public static final byte EXCEPTION = 4;
		public static final byte USER_DEFINED = 5;
		public static final byte THREAD_START = 6;
		public static final byte THREAD_DEATH = 7; // THREAD_DEATH = THREAD_END
		public static final byte THREAD_END = 7;
		public static final byte CLASS_PREPARE = 8;
		public static final byte CLASS_UNLOAD = 9;
		public static final byte CLASS_LOAD = 10;
		public static final byte FIELD_ACCESS = 20;
		public static final byte FIELD_MODIFICATION = 21;
		public static final byte EXCEPTION_CATCH = 30;
		public static final byte METHOD_ENTRY = 40;
		public static final byte METHOD_EXIT = 41;
		public static final byte METHOD_EXIT_WITH_RETURN_VALUE = 42;
		public static final byte MONITOR_CONTENDED_ENTER = 43;
		public static final byte MONITOR_CONTENDED_ENTERED = 44;
		public static final byte MONITOR_WAIT = 45;
		public static final byte MONITOR_WAITED = 46;
		public static final byte VM_INIT = 90; // VM_INIT = VM_START
		public static final byte VM_START = 90;
		public static final byte VM_DEATH = 99;
		public static final byte VM_DISCONNECTED = 100;
	}

	static class StepDepth {
		public static final byte INTO = 0;
		public static final byte OVER = 1;
		public static final byte OUT = 2;
	}

	static class StepSize {
		public static final byte MIN = 0;
		public static final byte LINE = 1;
	}

	static class SuspendPolicy {
		public static final byte NONE = 0;
		public static final byte EVENT_THREAD = 1;
		public static final byte ALL = 2;
	}

	static class ThreadStatus {
		public static final byte ZOMBIE = 0;
		public static final byte RUNNING = 1;
		public static final byte SLEEPING = 2;
		public static final byte MONITOR = 3;
		public static final byte WAIT = 4;
	}

	static class TypeTag  {
		public static final byte CLASS = 1;
		public static final byte INTERFACE = 2;
		public static final byte ARRAY = 3;
	}
}
